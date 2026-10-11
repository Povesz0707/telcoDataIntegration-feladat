package hu.telcodata.integration.normalization;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SkuNormalizer {

    public String normalize(String rawSku) {
        if (rawSku == null || rawSku.isBlank()) {
            return "";
        }
        String compact = rawSku.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        if (compact.startsWith("SKU")) {
            return compact.substring(3);
        }
        if (compact.startsWith("P")) {
            return compact.substring(1);
        }
        return compact;
    }

    public String normalizeManufacturer(String manufacturer) {
        if (manufacturer == null) {
            return "";
        }
        return manufacturer.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }
}
