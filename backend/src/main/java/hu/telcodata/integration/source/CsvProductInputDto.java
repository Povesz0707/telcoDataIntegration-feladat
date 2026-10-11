package hu.telcodata.integration.source;

import java.math.BigDecimal;

public record CsvProductInputDto(
        String sku,
        String productName,
        BigDecimal grossPriceHuf,
        Integer stockQty,
        String brand,
        String rawData
) {
}
