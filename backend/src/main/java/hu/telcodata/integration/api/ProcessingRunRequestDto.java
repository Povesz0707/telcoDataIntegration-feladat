package hu.telcodata.integration.api;

import hu.telcodata.integration.constant.MessageConstants;
import hu.telcodata.integration.domain.enums.SourceType;
import jakarta.validation.constraints.NotNull;

public record ProcessingRunRequestDto(
        @NotNull Long csvBatchId,
        @NotNull Long jsonBatchId,
        @NotNull(message = MessageConstants.PRIMARY_SOURCE_REQUIRED) SourceType primarySource,
        boolean fallbackMissingValues
) {
}
