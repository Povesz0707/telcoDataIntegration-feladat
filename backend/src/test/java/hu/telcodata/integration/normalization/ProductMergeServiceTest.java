package hu.telcodata.integration.normalization;

import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.enums.IssueCode;
import hu.telcodata.integration.domain.enums.IssueSeverity;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.domain.enums.UploadStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMergeServiceTest {

    private final ProductMergeService service = new ProductMergeService(new SkuNormalizer());

    @Test
    void csvPrimaryMarksDuplicatePrimaryRecordInvalid() {
        List<NormalizedProduct> products = service.merge(csvRecords(), jsonRecords(), SourceType.CSV, true);

        assertThat(products).hasSize(4);
        assertThat(products).filteredOn(NormalizedProduct::isValid).hasSize(1);
        assertThat(products.stream()
                .filter(product -> "P-1002".equals(product.getSku()))
                .findFirst()
                .orElseThrow()
                .getIssues())
                .anyMatch(issue -> IssueCode.DUPLICATE_PRIMARY_SOURCE == issue.getCode());
    }

    @Test
    void jsonPrimaryUsesCsvStockAsFallbackAndKeepsOnlyRealErrorsInvalid() {
        List<NormalizedProduct> products = service.merge(csvRecords(), jsonRecords(), SourceType.JSON, true);

        assertThat(products).hasSize(4);
        assertThat(products).filteredOn(NormalizedProduct::isValid).hasSize(2);
        NormalizedProduct product = products.stream()
                .filter(item -> "SKU1002".equals(item.getSku()))
                .findFirst()
                .orElseThrow();
        assertThat(product.isValid()).isTrue();
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(product.getIssues()).anyMatch(issue -> IssueCode.FALLBACK_VALUE_USED == issue.getCode());
    }

    @Test
    void missingRequiredFieldMakesProductInvalid() {
        NormalizedProduct product = service.merge(
                        List.of(csvRecord("P-2001", "Widget", null)),
                        List.of(),
                        SourceType.CSV,
                        false)
                .getFirst();

        assertThat(product.isValid()).isFalse();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.MISSING_REQUIRED_FIELD
                        && ProductFieldNames.STOCK.equals(issue.getField())
                        && issue.getSeverity() == IssueSeverity.ERROR);
    }

    @Test
    void missingSkuIsAnErrorEvenWhenTheOtherFieldsArePresent() {
        NormalizedProduct product = service.merge(
                        List.of(new CsvProductSource(csvBatch(), 2, null, "Widget",
                                new BigDecimal("12700"), 1, "ACME", "{}")),
                        List.of(),
                        SourceType.CSV,
                        false)
                .getFirst();

        assertThat(product.isValid()).isFalse();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.MISSING_SKU
                        && issue.getSeverity() == IssueSeverity.ERROR);
    }

    @Test
    void disabledFallbackDoesNotHideMissingPrimaryValue() {
        NormalizedProduct product = service.merge(
                        List.of(csvRecord("P-2002", "Widget", 8)),
                        List.of(jsonRecord("SKU2002", "Widget", null)),
                        SourceType.JSON,
                        false)
                .getFirst();

        assertThat(product.getStock()).isNull();
        assertThat(product.isValid()).isFalse();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.MISSING_REQUIRED_FIELD
                        && ProductFieldNames.STOCK.equals(issue.getField())
                        && issue.getSeverity() == IssueSeverity.ERROR);
        assertThat(product.getIssues()).noneMatch(issue -> issue.getCode() == IssueCode.FALLBACK_VALUE_USED);
    }

    @Test
    void sourceConflictIsAWarningWhenTheNormalizedRecordIsComplete() {
        NormalizedProduct product = service.merge(
                        List.of(csvRecord("P-2003", "Widget", 8)),
                        List.of(jsonRecord("SKU2003", "Widget", 9)),
                        SourceType.CSV,
                        false)
                .getFirst();

        assertThat(product.isValid()).isTrue();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.SOURCE_CONFLICT
                        && ProductFieldNames.STOCK.equals(issue.getField())
                        && issue.getSeverity() == IssueSeverity.WARNING);
    }

    @Test
    void duplicateSecondaryRecordIsAWarningOnly() {
        NormalizedProduct product = service.merge(
                        List.of(csvRecord("P-2004", "Widget", 8), csvRecord("P-2004", "Widget", 7)),
                        List.of(jsonRecord("SKU2004", "Widget", 9)),
                        SourceType.JSON,
                        false)
                .getFirst();

        assertThat(product.isValid()).isTrue();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.DUPLICATE_SECONDARY_SOURCE
                        && issue.getSeverity() == IssueSeverity.WARNING);
    }

    @Test
    void unsupportedCurrencyMakesTheProductInvalid() {
        JsonProductSource record = new JsonProductSource(
                jsonBatch(), 1, "SKU-2005", "Widget", new BigDecimal("100"), "EUR",
                new BigDecimal("0.27"), 1, "ACME", "ean-2005",
                OffsetDateTime.now(ZoneOffset.UTC), "{}");

        NormalizedProduct product = service.merge(List.of(), List.of(record), SourceType.JSON, false).getFirst();

        assertThat(product.isValid()).isFalse();
        assertThat(product.getIssues())
                .anyMatch(issue -> issue.getCode() == IssueCode.UNSUPPORTED_CURRENCY
                        && issue.getSeverity() == IssueSeverity.ERROR);
    }

    private CsvProductSource csvRecord(String sku, String name, Integer stock) {
        return new CsvProductSource(csvBatch(), 2, sku, name, new BigDecimal("12700"), stock, "ACME", "{}");
    }

    private JsonProductSource jsonRecord(String sku, String name, Integer stock) {
        return new JsonProductSource(jsonBatch(), 1, sku, name, new BigDecimal("10000"), "HUF",
                new BigDecimal("0.27"), stock, "ACME", "ean-1",
                OffsetDateTime.now(ZoneOffset.UTC), "{}");
    }

    private UploadBatch csvBatch() {
        return new UploadBatch(SourceType.CSV, "test.csv", "csv", Instant.now(), UploadStatus.COMPLETED, 1);
    }

    private UploadBatch jsonBatch() {
        return new UploadBatch(SourceType.JSON, "test.json", "json", Instant.now(), UploadStatus.COMPLETED, 1);
    }

    private List<CsvProductSource> csvRecords() {
        UploadBatch batch = new UploadBatch(SourceType.CSV, "test.csv", "csv", Instant.now(), UploadStatus.COMPLETED, 4);
        return List.of(
                new CsvProductSource(batch, 2, "P-1001", "Widget", new BigDecimal("12700"), 15, "ACME", "{}"),
                new CsvProductSource(batch, 3, "P-1002", "Widget Pro", new BigDecimal("25400"), 8, "ACME", "{}"),
                new CsvProductSource(batch, 4, "P-1002", "Widget Pro", new BigDecimal("25400"), 7, "ACME", "{}"),
                new CsvProductSource(batch, 5, "P-1005", "Cable", new BigDecimal("3175"), null, "Wire&Co", "{}")
        );
    }

    private List<JsonProductSource> jsonRecords() {
        UploadBatch batch = new UploadBatch(SourceType.JSON, "test.json", "json", Instant.now(), UploadStatus.COMPLETED, 3);
        OffsetDateTime updatedAt = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        return List.of(
                new JsonProductSource(batch, 1, "SKU1001", "Widget", new BigDecimal("10000"), "HUF",
                        new BigDecimal("0.27"), 14, "ACME", "ean-1", updatedAt, "{}"),
                new JsonProductSource(batch, 2, "SKU1002", "Widget Pro", new BigDecimal("20000"), "HUF",
                        new BigDecimal("0.27"), null, "ACME", "ean-2", updatedAt, "{}"),
                new JsonProductSource(batch, 3, "SKU-XYZ", "Unknown", null, "HUF",
                        new BigDecimal("0.27"), 1, null, null, updatedAt, "{}")
        );
    }
}
