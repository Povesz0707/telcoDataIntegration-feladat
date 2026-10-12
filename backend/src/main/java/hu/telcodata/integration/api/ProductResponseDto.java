package hu.telcodata.integration.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponseDto(
        String sku,
        String name,
        String manufacturer,
        BigDecimal finalPriceHuf,
        Integer stock,
        String ean,
        Instant updatedAt,
        String source,
        String primarySource,
        boolean valid,
        List<ProductIssueResponseDto> issues
) {
}
