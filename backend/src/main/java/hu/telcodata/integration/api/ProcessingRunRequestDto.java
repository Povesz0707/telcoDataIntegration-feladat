package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.SourceType;
import jakarta.validation.constraints.NotNull;

public record ProcessingRunRequestDto(
        @NotNull Long csvBatchId,
        @NotNull Long jsonBatchId,
        @NotNull SourceType primarySource,
        boolean fallbackMissingValues
) {
}
