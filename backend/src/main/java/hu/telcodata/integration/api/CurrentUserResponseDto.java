package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CurrentUserResponse", description = "Az aktuális autentikált felhasználó adatai")
public record CurrentUserResponseDto(
        @Schema(description = "Felhasználónév", example = "telco_admin")
        String username,
        @Schema(description = "Felhasználói szerepkör", example = "ADMIN")
        UserRole role
) {
}
