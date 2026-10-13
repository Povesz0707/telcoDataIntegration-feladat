package hu.telcodata.integration.security;

import hu.telcodata.integration.config.InitialUserProperties;
import hu.telcodata.integration.domain.AppUser;
import hu.telcodata.integration.domain.enums.UserRole;
import hu.telcodata.integration.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialUserProperties properties;

    @Override
    @Transactional
    public void run(String... args) {
        initializeAdminUser();
        initializeUser();
    }

    private void initializeAdminUser() {
        InitialUserProperties.UserCredentials credentials = properties.admin();
        if (userRepository.findByUsername(credentials.username()).isPresent()) {
            return;
        }

        if (userRepository.count() == 1 && migrateLegacyAdminIfApplicable(credentials)) {
            return;
        }

        userRepository.save(new AppUser(credentials.username(), passwordEncoder.encode(credentials.password()),
                UserRole.ADMIN));
    }

    private boolean migrateLegacyAdminIfApplicable(InitialUserProperties.UserCredentials credentials) {
        return userRepository.findByUsername("admin")
                .map(legacyAdmin -> {
                    legacyAdmin.changeCredentials(credentials.username(),
                            passwordEncoder.encode(credentials.password()), UserRole.ADMIN);
                    userRepository.save(legacyAdmin);
                    return true;
                })
                .orElse(false);
    }

    private void initializeUser() {
        InitialUserProperties.UserCredentials credentials = properties.user();
        if (userRepository.findByUsername(credentials.username()).isEmpty()) {
            userRepository.save(new AppUser(credentials.username(), passwordEncoder.encode(credentials.password()),
                    UserRole.USER));
        }
    }
}
