package com.bookstore.dto;

import com.bookstore.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record UserResponse(Long id, String email, String displayName) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
        }
    }

    public record AuthResponse(UserResponse user, String accessToken, long expiresIn) {
    }
}
