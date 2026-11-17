package com.bookstore.security;

public record AuthPrincipal(Long id, String email, String role) {
}
