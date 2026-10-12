package hu.telcodata.integration.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum ProductField {
    SKU("sku"),
    NAME("name"),
    MANUFACTURER("manufacturer"),
    FINAL_PRICE_HUF("finalPriceHuf"),
    STOCK("stock"),
    EAN("ean"),
    UPDATED_AT("updatedAt"),
    SOURCE("source"),
    VALID("valid");

    private final String propertyName;

    ProductField(String propertyName) {
        this.propertyName = propertyName;
    }

    @JsonValue
    public String propertyName() {
        return propertyName;
    }

    public static Optional<ProductField> findByPropertyName(String value) {
        return Arrays.stream(values())
                .filter(field -> field.propertyName.equals(value))
                .findFirst();
    }
}
