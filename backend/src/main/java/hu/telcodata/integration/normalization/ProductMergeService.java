package hu.telcodata.integration.normalization;

import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.IssueSeverity;
import hu.telcodata.integration.domain.IssueCode;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.SourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductMergeService {

    private final SkuNormalizer skuNormalizer;

    public List<NormalizedProduct> merge(List<CsvProductSource> csvRecords,
                                         List<JsonProductSource> jsonRecords,
                                         SourceType primarySource,
                                         boolean fallbackMissingValues) {
        Map<SourceType, Map<String, List<SourceCandidate>>> candidates = new EnumMap<>(SourceType.class);
        candidates.put(SourceType.CSV, groupCsv(csvRecords));
        candidates.put(SourceType.JSON, groupJson(jsonRecords));

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(candidates.get(SourceType.CSV).keySet());
        keys.addAll(candidates.get(SourceType.JSON).keySet());

        return keys.stream()
                .map(key -> mergeOne(key, candidates, primarySource, fallbackMissingValues))
                .toList();
    }

    private NormalizedProduct mergeOne(String key,
                                       Map<SourceType, Map<String, List<SourceCandidate>>> candidates,
                                       SourceType primarySource,
                                       boolean fallbackMissingValues) {
        List<SourceCandidate> primary = candidates.get(primarySource).getOrDefault(key, List.of());
        SourceType secondarySource = otherSource(primarySource);
        List<SourceCandidate> secondary = candidates.get(secondarySource).getOrDefault(key, List.of());
        SourceCandidate primaryCandidate = first(primary);
        SourceCandidate secondaryCandidate = first(secondary);
        SourceCandidate base = primaryCandidate != null ? primaryCandidate : secondaryCandidate;

        ProductMergeValues values = resolveValues(base, primaryCandidate, secondaryCandidate,
                fallbackMissingValues || primaryCandidate == null);
        String source = primaryCandidate != null && secondaryCandidate != null
                ? "CSV+JSON"
                : base == null ? primarySource.name() : base.sourceType().name();

        NormalizedProduct product = new NormalizedProduct(
                displaySku(base, key), values.name(), values.manufacturer(), values.finalPriceHuf(),
                values.stock(), values.ean(), values.updatedAt(), source, primarySource.name());

        if (base == null || isBlank(base.rawSku())) {
            product.addIssue(IssueCode.MISSING_SKU, ProductFieldNames.SKU,
                    "The source record has no SKU", IssueSeverity.ERROR);
        }
        if (primary.size() > 1) {
            product.addIssue(IssueCode.DUPLICATE_PRIMARY_SOURCE, ProductFieldNames.SKU,
                    "The primary source contains multiple records for the same SKU", IssueSeverity.ERROR);
        } else if (secondary.size() > 1) {
            product.addIssue(IssueCode.DUPLICATE_SECONDARY_SOURCE, ProductFieldNames.SKU,
                    "The secondary source contains multiple records for the same SKU", IssueSeverity.WARNING);
        }
        if (primaryCandidate == null || secondaryCandidate == null) {
            product.addIssue(IssueCode.SOURCE_ONLY_RECORD, ProductFieldNames.SOURCE,
                    "The record exists in only one source", IssueSeverity.WARNING);
        }
        addSourceDataIssue(product, base);
        addFallbackIssues(product, primaryCandidate, secondaryCandidate, values, fallbackMissingValues);
        addConflictIssues(product, primaryCandidate, secondaryCandidate);
        addRequiredFieldIssues(product, values);
        product.markValid(product.getIssues().stream().noneMatch(issue -> issue.getSeverity() == IssueSeverity.ERROR));
        return product;
    }

    private ProductMergeValues resolveValues(SourceCandidate base, SourceCandidate primary,
                                             SourceCandidate secondary, boolean allowFallback) {
        return new ProductMergeValues(
                resolve(primary, secondary, SourceCandidate::name, allowFallback),
                resolve(primary, secondary, SourceCandidate::manufacturer, allowFallback),
                resolve(primary, secondary, SourceCandidate::finalPriceHuf, allowFallback),
                resolve(primary, secondary, SourceCandidate::stock, allowFallback),
                resolve(primary, secondary, SourceCandidate::ean, allowFallback),
                resolve(primary, secondary, SourceCandidate::updatedAt, allowFallback));
    }

    private <T> T resolve(SourceCandidate primary, SourceCandidate secondary,
                           Function<SourceCandidate, T> extractor, boolean allowFallback) {
        if (primary != null) {
            T primaryValue = extractor.apply(primary);
            if (hasValue(primaryValue) || !allowFallback || secondary == null) {
                return primaryValue;
            }
        }
        return secondary == null ? null : extractor.apply(secondary);
    }

    private void addFallbackIssues(NormalizedProduct product, SourceCandidate primary,
                                   SourceCandidate secondary, ProductMergeValues values,
                                   boolean fallbackEnabled) {
        if (!fallbackEnabled || primary == null || secondary == null) {
            return;
        }
        addFallbackIfUsed(product, ProductFieldNames.NAME, primary.name(), secondary.name(), values.name());
        addFallbackIfUsed(product, ProductFieldNames.MANUFACTURER,
                primary.manufacturer(), secondary.manufacturer(), values.manufacturer());
        addFallbackIfUsed(product, ProductFieldNames.FINAL_PRICE_HUF,
                primary.finalPriceHuf(), secondary.finalPriceHuf(), values.finalPriceHuf());
        addFallbackIfUsed(product, ProductFieldNames.STOCK, primary.stock(), secondary.stock(), values.stock());
        addFallbackIfUsed(product, ProductFieldNames.EAN, primary.ean(), secondary.ean(), values.ean());
        addFallbackIfUsed(product, ProductFieldNames.UPDATED_AT,
                primary.updatedAt(), secondary.updatedAt(), values.updatedAt());
    }

    private void addFallbackIfUsed(NormalizedProduct product, String field, Object primary,
                                   Object secondary, Object resolved) {
        if (!hasValue(primary) && hasValue(secondary) && Objects.equals(resolved, secondary)) {
            product.addIssue(IssueCode.FALLBACK_VALUE_USED, field,
                    "The value was filled from the secondary source", IssueSeverity.WARNING);
        }
    }

    private void addConflictIssues(NormalizedProduct product, SourceCandidate primary,
                                   SourceCandidate secondary) {
        if (primary == null || secondary == null) {
            return;
        }
        addConflict(product, ProductFieldNames.FINAL_PRICE_HUF, primary.finalPriceHuf(), secondary.finalPriceHuf());
        addConflict(product, ProductFieldNames.STOCK, primary.stock(), secondary.stock());
        addConflict(product, ProductFieldNames.EAN, primary.ean(), secondary.ean());
        if (!isBlank(primary.manufacturer()) && !isBlank(secondary.manufacturer())
                && !skuNormalizer.normalizeManufacturer(primary.manufacturer())
                .equals(skuNormalizer.normalizeManufacturer(secondary.manufacturer()))) {
            addConflict(product, ProductFieldNames.MANUFACTURER, primary.manufacturer(), secondary.manufacturer());
        }
    }

    private void addConflict(NormalizedProduct product, String field, Object primary, Object secondary) {
        if (hasValue(primary) && hasValue(secondary) && !Objects.equals(primary, secondary)) {
            if (primary instanceof BigDecimal primaryDecimal && secondary instanceof BigDecimal secondaryDecimal
                    && primaryDecimal.compareTo(secondaryDecimal) == 0) {
                return;
            }
            product.addIssue(IssueCode.SOURCE_CONFLICT, field,
                    "Primary source value differs from the secondary source", IssueSeverity.WARNING);
        }
    }

    private void addRequiredFieldIssues(NormalizedProduct product, ProductMergeValues values) {
        addRequired(product, ProductFieldNames.NAME, values.name());
        addRequired(product, ProductFieldNames.MANUFACTURER, values.manufacturer());
        addRequired(product, ProductFieldNames.FINAL_PRICE_HUF, values.finalPriceHuf());
        addRequired(product, ProductFieldNames.STOCK, values.stock());
    }

    private void addRequired(NormalizedProduct product, String field, Object value) {
        if (!hasValue(value)) {
            product.addIssue(IssueCode.MISSING_REQUIRED_FIELD, field,
                    "A required value is missing", IssueSeverity.ERROR);
        }
    }

    private void addSourceDataIssue(NormalizedProduct product, SourceCandidate candidate) {
        if (candidate != null && candidate.sourceDataIssue() != null) {
            product.addIssue(candidate.sourceDataIssue(), ProductFieldNames.FINAL_PRICE_HUF,
                    "The source value could not be converted to HUF", IssueSeverity.ERROR);
        }
    }

    private String displaySku(SourceCandidate base, String key) {
        return base == null || isBlank(base.rawSku()) ? null : base.rawSku().trim();
    }

    private Map<String, List<SourceCandidate>> groupCsv(List<CsvProductSource> records) {
        return records.stream()
                .map(record -> new SourceCandidate(SourceType.CSV, record.getSku(),
                        skuNormalizer.normalize(record.getSku()), record.getProductName(), record.getBrand(),
                        record.getGrossPriceHuf(), record.getStockQty(), null, null, null,
                        record.getRowNumber()))
                .collect(Collectors.groupingBy(this::groupKey, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<SourceCandidate>> groupJson(List<JsonProductSource> records) {
        return records.stream()
                .map(record -> {
                    boolean huf = record.getCurrency() == null || "HUF".equalsIgnoreCase(record.getCurrency());
                    BigDecimal gross = huf && record.getNetPrice() != null && record.getVatRate() != null
                            ? record.getNetPrice().multiply(BigDecimal.ONE.add(record.getVatRate()))
                            .setScale(0, RoundingMode.HALF_UP)
                            : null;
                    return new SourceCandidate(SourceType.JSON, record.getSourceId(),
                            skuNormalizer.normalize(record.getSourceId()), record.getName(), record.getManufacturer(),
                            gross, record.getQuantityAvailable(), record.getEan(),
                            record.getUpdatedAt() == null ? null : record.getUpdatedAt().toInstant(),
                            huf ? null : IssueCode.UNSUPPORTED_CURRENCY, record.getRowNumber());
                })
                .collect(Collectors.groupingBy(this::groupKey, LinkedHashMap::new, Collectors.toList()));
    }

    private String groupKey(SourceCandidate candidate) {
        return candidate.normalizedSku().isBlank()
                ? candidate.sourceType().name() + "_ROW_" + candidate.rowNumber()
                : candidate.normalizedSku();
    }

    private SourceCandidate first(List<SourceCandidate> candidates) {
        return candidates.stream().min(Comparator.comparingInt(SourceCandidate::rowNumber)).orElse(null);
    }

    private SourceType otherSource(SourceType sourceType) {
        return sourceType == SourceType.CSV ? SourceType.JSON : SourceType.CSV;
    }

    private boolean hasValue(Object value) {
        return value != null && (!(value instanceof String string) || !string.isBlank());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProductMergeValues(String name, String manufacturer, BigDecimal finalPriceHuf,
                                      Integer stock, String ean, java.time.Instant updatedAt) {
    }

}
