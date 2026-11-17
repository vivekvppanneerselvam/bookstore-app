package com.bookstore.controller;

import com.bookstore.dto.AuthDtos.AuthResponse;
import com.bookstore.dto.AuthDtos.LoginRequest;
import com.bookstore.dto.AuthDtos.RegisterRequest;
import com.bookstore.dto.AuthDtos.UserResponse;
import com.bookstore.security.AuthPrincipal;
import com.bookstore.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final boolean secureCookie;

    public AuthController(AuthService authService, @Value("${app.security.cookie-secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshTtlSeconds()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshTtlSeconds()).toString());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshTtlSeconds()).toString());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return authService.getUser(principal.id());
    }

    private AuthResponse toResponse(AuthService.IssuedTokens tokens) {
        return new AuthResponse(tokens.user(), tokens.accessToken(), tokens.accessTtlSeconds());
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
