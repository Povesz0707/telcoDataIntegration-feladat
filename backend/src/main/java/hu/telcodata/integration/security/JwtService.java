package hu.telcodata.integration.security;

import hu.telcodata.integration.config.JwtProperties;
import hu.telcodata.integration.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.secret()));
    }

    public String createAccessToken(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken() {
        byte[] value = new byte[48];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String hashRefreshToken(String refreshToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenExpiration().toSeconds();
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now().plus(properties.refreshTokenExpiration());
    }
}
