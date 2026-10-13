package hu.telcodata.integration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Felhasználói bejelentkezési adatok")
public record LoginRequestDto(
        @Schema(description = "Felhasználónév", example = "telco_admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String username,
        @Schema(description = "Jelszó", example = "telco_admin12345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {
}
