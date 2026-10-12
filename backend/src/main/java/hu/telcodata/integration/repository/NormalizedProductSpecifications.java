package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.normalization.ProductFieldNames;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NormalizedProductSpecifications {

    private NormalizedProductSpecifications() {
    }

    public static Specification<NormalizedProduct> byQuery(Long runId, String filter, String sku,
                                                            String name, String manufacturer,
                                                            String finalPriceHuf, String stock,
                                                            String ean, String source, Boolean valid,
                                                            boolean onlyValid) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("processingRun").get("id"), runId));
            addGlobalFilter(predicates, root, builder, filter);
            addTextFilter(predicates, root, builder, ProductFieldNames.SKU, sku);
            addTextFilter(predicates, root, builder, ProductFieldNames.NAME, name);
            addTextFilter(predicates, root, builder, ProductFieldNames.MANUFACTURER, manufacturer);
            addDecimalFilter(predicates, root, builder, ProductFieldNames.FINAL_PRICE_HUF, finalPriceHuf);
            addIntegerFilter(predicates, root, builder, ProductFieldNames.STOCK, stock);
            addTextFilter(predicates, root, builder, ProductFieldNames.EAN, ean);
            addExactTextFilter(predicates, root, builder, ProductFieldNames.SOURCE, source);
            if (valid != null) {
                predicates.add(builder.equal(root.get(ProductFieldNames.VALID), valid));
            }
            if (onlyValid) {
                predicates.add(builder.isTrue(root.get(ProductFieldNames.VALID)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addGlobalFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                        CriteriaBuilder builder, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        String pattern = containsPattern(normalizedFilter);
        predicates.add(builder.or(
                contains(builder, root.get(ProductFieldNames.SKU), pattern),
                contains(builder, root.get(ProductFieldNames.NAME), pattern),
                contains(builder, root.get(ProductFieldNames.MANUFACTURER), pattern),
                contains(builder, root.get(ProductFieldNames.EAN), pattern)));
    }

    private static void addTextFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                      CriteriaBuilder builder, String field, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNotAvailable(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(field)));
            return;
        }
        predicates.add(contains(builder, root.get(field), containsPattern(normalizedFilter)));
    }

    private static void addExactTextFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                           CriteriaBuilder builder, String field, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNotAvailable(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(field)));
            return;
        }
        predicates.add(builder.equal(builder.lower(root.get(field)), normalizedFilter));
    }

    private static void addDecimalFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                         CriteriaBuilder builder, String field, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNotAvailable(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(field)));
            return;
        }
        try {
            BigDecimal value = new BigDecimal(normalizedFilter.replace(" ", "").replace(',', '.'));
            predicates.add(builder.equal(root.get(field), value));
        } catch (NumberFormatException exception) {
            predicates.add(builder.disjunction());
        }
    }

    private static void addIntegerFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                         CriteriaBuilder builder, String field, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNotAvailable(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(field)));
            return;
        }
        try {
            predicates.add(builder.equal(root.get(field), Integer.valueOf(normalizedFilter)));
        } catch (NumberFormatException exception) {
            predicates.add(builder.disjunction());
        }
    }

    private static Predicate contains(CriteriaBuilder builder, jakarta.persistence.criteria.Expression<?> expression,
                                      String pattern) {
        return builder.like(builder.lower(expression.as(String.class)), pattern);
    }

    private static String containsPattern(String value) {
        return "%" + value + "%";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isNotAvailable(String value) {
        String compact = value.replaceAll("[\\s./_-]", "");
        return compact.equals("na") || compact.equals("notavailable");
    }
}
