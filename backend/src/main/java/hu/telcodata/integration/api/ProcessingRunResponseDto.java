package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.ProcessingStatus;
import hu.telcodata.integration.domain.enums.SourceType;

import java.time.Instant;

public record ProcessingRunResponseDto(
        Long runId,
        SourceType primarySource,
        boolean fallbackMissingValues,
        ProcessingStatus status,
        Instant startedAt,
        Instant finishedAt,
        int totalCount,
        int validCount,
        int invalidCount,
        String csvFilename,
        String jsonFilename
) {
}
