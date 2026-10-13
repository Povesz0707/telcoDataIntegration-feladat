package hu.telcodata.integration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshTokenRequest", description = "Refresh token művelet kérésadata")
public record RefreshTokenRequestDto(
        @Schema(description = "A korábban kiadott refresh token", example = "eyJhbGciOiJIUzI1NiJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String refreshToken
) {
}
