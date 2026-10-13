package hu.telcodata.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.initial-users")
public record InitialUserProperties(
        UserCredentials admin,
        UserCredentials user
) {

    public record UserCredentials(String username, String password) {
    }
}
