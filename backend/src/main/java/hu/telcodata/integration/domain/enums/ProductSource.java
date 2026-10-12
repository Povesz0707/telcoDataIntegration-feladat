package hu.telcodata.integration.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum ProductSource {
    CSV("CSV"),
    JSON("JSON"),
    CSV_JSON("CSV+JSON");

    private final String value;

    ProductSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Optional<ProductSource> findByValue(String value) {
        return Arrays.stream(values())
                .filter(source -> source.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
