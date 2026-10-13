package hu.telcodata.integration.controller;

import hu.telcodata.integration.api.AuthResponseDto;
import hu.telcodata.integration.api.CurrentUserResponseDto;
import hu.telcodata.integration.api.LoginRequestDto;
import hu.telcodata.integration.api.RefreshTokenRequestDto;
import hu.telcodata.integration.domain.enums.UserRole;
import hu.telcodata.integration.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDto> currentUser(Authentication authentication) {
        UserRole role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.USER);
        return ResponseEntity.ok(new CurrentUserResponseDto(authentication.getName(), role));
    }
}
