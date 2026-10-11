package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.SourceType;

import java.time.Instant;

public record UploadResponseDto(
        Long batchId,
        SourceType sourceType,
        String originalFilename,
        int rowCount,
        Instant uploadedAt
) {
}
