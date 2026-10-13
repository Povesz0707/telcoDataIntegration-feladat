package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.UserRole;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        String username,
        UserRole role
) {
}
