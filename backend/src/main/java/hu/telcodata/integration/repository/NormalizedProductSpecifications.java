package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.enums.ProductField;
import hu.telcodata.integration.domain.enums.ProductSource;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.JpaExpression;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NormalizedProductSpecifications {

    private NormalizedProductSpecifications() {
    }

    public static Specification<NormalizedProduct> byQuery(Long runId, String filter, String sku,
                                                            String name, String manufacturer,
                                                            String finalPriceHuf, String stock,
                                                            String ean, String updatedAt, String source, String valid,
                                                            boolean onlyValid) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("processingRun").get("id"), runId));
            addGlobalFilter(predicates, root, builder, filter);
            addTextFilter(predicates, root, builder, ProductField.SKU, sku);
            addTextFilter(predicates, root, builder, ProductField.NAME, name);
            addTextFilter(predicates, root, builder, ProductField.MANUFACTURER, manufacturer);
            addDecimalFilter(predicates, root, builder, ProductField.FINAL_PRICE_HUF, finalPriceHuf);
            addIntegerFilter(predicates, root, builder, ProductField.STOCK, stock);
            addTextFilter(predicates, root, builder, ProductField.EAN, ean);
            addDateFilter(predicates, root, builder, updatedAt);
            addSourceFilter(predicates, root, builder, source);
            addBooleanFilter(predicates, root, builder, valid);
            if (onlyValid) {
                predicates.add(builder.isTrue(root.get(ProductField.VALID.propertyName())));
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
                contains(builder, root.get(ProductField.SKU.propertyName()), pattern),
                contains(builder, root.get(ProductField.NAME.propertyName()), pattern),
                contains(builder, root.get(ProductField.MANUFACTURER.propertyName()), pattern),
                contains(builder, root.get(ProductField.EAN.propertyName()), pattern)));
    }

    private static void addTextFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                      CriteriaBuilder builder, ProductField field, String filter) {
        String normalizedFilter = normalize(filter);
        String propertyName = field.propertyName();
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(propertyName)));
            return;
        }
        predicates.add(contains(builder, root.get(propertyName), containsPattern(normalizedFilter)));
    }

    private static void addSourceFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                        CriteriaBuilder builder, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(ProductField.SOURCE.propertyName())));
            return;
        }
        ProductSource.findByValue(normalizedFilter)
                .ifPresentOrElse(source -> predicates.add(
                                builder.equal(root.get(ProductField.SOURCE.propertyName()), source)),
                        () -> predicates.add(builder.disjunction()));
    }

    private static void addDateFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                      CriteriaBuilder builder, String filter) {
        String normalizedFilter = normalize(filter);
        String propertyName = ProductField.UPDATED_AT.propertyName();
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(propertyName)));
            return;
        }
        try {
            LocalDate date = LocalDate.parse(normalizedFilter);
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            predicates.add(builder.greaterThanOrEqualTo(root.<Instant>get(propertyName), start));
            predicates.add(builder.lessThan(root.<Instant>get(propertyName), end));
        } catch (DateTimeParseException exception) {
            predicates.add(builder.disjunction());
        }
    }

    private static void addDecimalFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                         CriteriaBuilder builder, ProductField field, String filter) {
        String normalizedFilter = normalize(filter);
        String propertyName = field.propertyName();
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(propertyName)));
            return;
        }
        try {
            BigDecimal value = new BigDecimal(normalizedFilter.replace(" ", "").replace(',', '.'));
            JpaExpression<?> numericExpression = (JpaExpression<?>) root.get(propertyName);
            predicates.add(builder.like(numericExpression.cast(String.class), containsPattern(value.toPlainString())));
        } catch (NumberFormatException exception) {
            predicates.add(builder.disjunction());
        }
    }

    private static void addBooleanFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                         CriteriaBuilder builder, String filter) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(ProductField.VALID.propertyName())));
            return;
        }
        if ("true".equals(normalizedFilter) || "false".equals(normalizedFilter)) {
            predicates.add(builder.equal(root.get(ProductField.VALID.propertyName()),
                    Boolean.valueOf(normalizedFilter)));
            return;
        }
        predicates.add(builder.disjunction());
    }

    private static void addIntegerFilter(List<Predicate> predicates, Root<NormalizedProduct> root,
                                         CriteriaBuilder builder, ProductField field, String filter) {
        String normalizedFilter = normalize(filter);
        String propertyName = field.propertyName();
        if (normalizedFilter.isBlank()) {
            return;
        }
        if (isNullFilter(normalizedFilter)) {
            predicates.add(builder.isNull(root.get(propertyName)));
            return;
        }
        try {
            predicates.add(builder.equal(root.get(propertyName), Integer.valueOf(normalizedFilter)));
        } catch (NumberFormatException exception) {
            predicates.add(builder.disjunction());
        }
    }

    private static Predicate contains(CriteriaBuilder builder, jakarta.persistence.criteria.Expression<?> expression,
                                      String pattern) {
        return builder.like(builder.lower(asStringExpression(expression)), pattern);
    }

    private static Expression<String> asStringExpression(Expression<?> expression) {
        if (expression instanceof JpaExpression<?> jpaExpression) {
            return jpaExpression.cast(String.class);
        }
        return expression.as(String.class);
    }

    private static String containsPattern(String value) {
        return "%" + value + "%";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isNullFilter(String value) {
        String compact = value.replaceAll("[\\s./_-]", "");
        return compact.equals("null") || compact.equals("na") || compact.equals("notavailable");
    }
}
