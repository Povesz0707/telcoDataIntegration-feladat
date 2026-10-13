package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "JWT access és refresh token válasz")
public record AuthResponseDto(
        @Schema(description = "Rövid élettartamú JWT access token")
        String accessToken,
        @Schema(description = "Tokenfrissítéshez használható refresh token")
        String refreshToken,
        @Schema(description = "Az access token érvényessége másodpercben", example = "900")
        long accessTokenExpiresInSeconds,
        @Schema(description = "Bejelentkezett felhasználó neve", example = "telco_admin")
        String username,
        @Schema(description = "Felhasználói szerepkör", example = "ADMIN")
        UserRole role
) {
}
