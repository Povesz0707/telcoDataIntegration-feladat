package hu.telcodata.integration.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ApiErrorResponse", description = "Egységes API hiba válasz")
public record ApiErrorResponseDto(
        @Schema(description = "A hiba időpontja")
        Instant timestamp,
        @Schema(description = "HTTP státuszkód", example = "401")
        int status,
        @Schema(description = "HTTP státusz rövid megnevezése", example = "Unauthorized")
        String error,
        @Schema(description = "A hiba ember által olvasható leírása")
        String message,
        @Schema(description = "A hibát okozó kérés útvonala", example = "/api/auth/me")
        String path
) {
}
