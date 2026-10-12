package hu.telcodata.integration.api;

import java.util.List;

public record ProductPageResponseDto(
        List<ProductResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
