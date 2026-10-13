package hu.telcodata.integration.controller;

import hu.telcodata.integration.api.AuthResponseDto;
import hu.telcodata.integration.api.CurrentUserResponseDto;
import hu.telcodata.integration.api.LoginRequestDto;
import hu.telcodata.integration.api.RefreshTokenRequestDto;
import hu.telcodata.integration.domain.enums.UserRole;
import hu.telcodata.integration.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "JWT alapú bejelentkezés és munkamenet-kezelés")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Bejelentkezés", description = "Access és refresh token kiadása érvényes felhasználói adatok esetén.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sikeres bejelentkezés",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Hiányzó vagy hibás kérés",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Hibás felhasználónév vagy jelszó",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class)))
    })
    public ResponseEntity<AuthResponseDto> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bejelentkezési adatok",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDto.class)))
            @Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access token frissítése", description = "A refresh token rotációjával új access és refresh token kiadása.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sikeres tokenfrissítés",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Hiányzó vagy hibás refresh token",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Érvénytelen vagy lejárt refresh token",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class)))
    })
    public ResponseEntity<AuthResponseDto> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Érvényes refresh token",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RefreshTokenRequestDto.class)))
            @Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Kijelentkezés", description = "A megadott refresh token visszavonása.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sikeres kijelentkezés"),
            @ApiResponse(responseCode = "400", description = "Hiányzó refresh token",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class)))
    })
    public ResponseEntity<Void> logout(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Visszavonandó refresh token",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RefreshTokenRequestDto.class)))
            @Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Aktuális felhasználó", description = "A bearer tokenhez tartozó felhasználó és szerepkör lekérdezése.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aktuális felhasználó",
                    content = @Content(schema = @Schema(implementation = CurrentUserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Hiányzó vagy érvénytelen access token",
                    content = @Content(schema = @Schema(implementation = hu.telcodata.integration.api.ApiErrorResponseDto.class)))
    })
    public ResponseEntity<CurrentUserResponseDto> currentUser(Authentication authentication) {
        UserRole role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.USER);
        return ResponseEntity.ok(new CurrentUserResponseDto(authentication.getName(), role));
    }
}
