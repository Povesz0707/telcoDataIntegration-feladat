package hu.telcodata.integration.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import hu.telcodata.integration.domain.enums.ProductSource;
import hu.telcodata.integration.domain.enums.SourceType;

public record ProductResponseDto(
        String sku,
        String name,
        String manufacturer,
        BigDecimal finalPriceHuf,
        Integer stock,
        String ean,
        Instant updatedAt,
        ProductSource source,
        SourceType primarySource,
        boolean valid,
        List<ProductIssueResponseDto> issues
) {
}
