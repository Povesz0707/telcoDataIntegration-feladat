package hu.telcodata.integration.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import hu.telcodata.integration.domain.NormalizedProduct;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public enum ProductField {
    SKU("sku", NormalizedProduct::getSku),
    NAME("name", NormalizedProduct::getName),
    MANUFACTURER("manufacturer", NormalizedProduct::getManufacturer),
    FINAL_PRICE_HUF("finalPriceHuf", NormalizedProduct::getFinalPriceHuf),
    STOCK("stock", NormalizedProduct::getStock),
    EAN("ean", NormalizedProduct::getEan),
    UPDATED_AT("updatedAt", NormalizedProduct::getUpdatedAt),
    SOURCE("source", NormalizedProduct::getSource),
    VALID("valid", NormalizedProduct::isValid);

    private final String propertyName;
    private final Function<NormalizedProduct, Comparable<?>> sortValueExtractor;

    ProductField(String propertyName, Function<NormalizedProduct, Comparable<?>> sortValueExtractor) {
        this.propertyName = propertyName;
        this.sortValueExtractor = sortValueExtractor;
    }

    @JsonValue
    public String propertyName() {
        return propertyName;
    }

    public Comparator<NormalizedProduct> comparator() {
        return (left, right) -> {
            Comparable<?> leftValue = sortValueExtractor.apply(left);
            Comparable<?> rightValue = sortValueExtractor.apply(right);
            if (leftValue == rightValue) {
                return 0;
            }
            if (leftValue == null) {
                return 1;
            }
            if (rightValue == null) {
                return -1;
            }
            return compareValues(leftValue, rightValue);
        };
    }

    public Comparable<?> sortValue(NormalizedProduct product) {
        return sortValueExtractor.apply(product);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareValues(Comparable left, Comparable right) {
        return left.compareTo(right);
    }

    public static Optional<ProductField> findByPropertyName(String value) {
        return Arrays.stream(values())
                .filter(field -> field.propertyName.equals(value))
                .findFirst();
    }
}
