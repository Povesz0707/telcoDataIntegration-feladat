package hu.telcodata.integration.security;

import hu.telcodata.integration.api.AuthResponseDto;
import hu.telcodata.integration.api.LoginRequestDto;
import hu.telcodata.integration.api.RefreshTokenRequestDto;
import hu.telcodata.integration.domain.AppUser;
import hu.telcodata.integration.domain.RefreshToken;
import hu.telcodata.integration.exception.UnauthorizedException;
import hu.telcodata.integration.repository.AppUserRepository;
import hu.telcodata.integration.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";
    private static final String INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        AppUser user = userRepository.findByUsername(request.username())
                .filter(AppUser::isEnabled)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponseDto refresh(RefreshTokenRequestDto request) {
        String tokenHash = jwtService.hashRefreshToken(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
        if (!storedToken.isActive(Instant.now())) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        storedToken.revoke(Instant.now());
        return issueTokens(storedToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(request.refreshToken()))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthResponseDto issueTokens(AppUser user) {
        String refreshTokenValue = jwtService.createRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user, jwtService.hashRefreshToken(refreshTokenValue),
                Instant.now(), jwtService.refreshTokenExpiresAt()));
        return new AuthResponseDto(jwtService.createAccessToken(user), refreshTokenValue,
                jwtService.accessTokenExpiresInSeconds(), user.getUsername(), user.getRole());
    }
}
