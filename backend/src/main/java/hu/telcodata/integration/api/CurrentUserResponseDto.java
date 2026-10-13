package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.UserRole;

public record CurrentUserResponseDto(
        String username,
        UserRole role
) {
}
