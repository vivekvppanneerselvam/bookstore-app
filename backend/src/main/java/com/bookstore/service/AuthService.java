package com.bookstore.service;

import com.bookstore.common.ApiException;
import com.bookstore.common.Hashing;
import com.bookstore.dto.AuthDtos.LoginRequest;
import com.bookstore.dto.AuthDtos.RegisterRequest;
import com.bookstore.dto.AuthDtos.UserResponse;
import com.bookstore.model.RefreshToken;
import com.bookstore.model.User;
import com.bookstore.repository.RefreshTokenRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTtlSeconds;
    private final String dummyHash;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-token-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public IssuedTokens register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        return issueTokens(user);
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public IssuedTokens refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is missing");
        }
        RefreshToken token = refreshTokens.findByTokenHash(Hashing.sha256(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));

        if (token.isRevoked()) {
            // A revoked token being presented again means it was replayed: invalidate the whole family.
            refreshTokens.revokeAllForUser(token.getUser().getId());
            throw ApiException.unauthorized("REFRESH_TOKEN_REUSED", "Refresh token has already been used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        token.revoke();
        return issueTokens(token.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(Hashing.sha256(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User no longer exists"));
        return UserResponse.from(user);
    }

    private IssuedTokens issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String rawRefreshToken = generateRefreshToken();
        RefreshToken refreshToken = new RefreshToken(user, Hashing.sha256(rawRefreshToken),
                Instant.now().plusSeconds(refreshTtlSeconds));
        refreshTokens.save(refreshToken);
        return new IssuedTokens(UserResponse.from(user), accessToken, rawRefreshToken,
                jwtService.getAccessTtlSeconds(), refreshTtlSeconds);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedTokens(UserResponse user, String accessToken, String refreshToken, long accessTtlSeconds,
                               long refreshTtlSeconds) {
    }
}
