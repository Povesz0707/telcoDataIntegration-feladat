package hu.telcodata.integration.api;

import java.time.Instant;

public record ApiErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
