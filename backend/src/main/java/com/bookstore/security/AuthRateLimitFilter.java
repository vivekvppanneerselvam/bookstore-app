package com.bookstore.security;

import com.bookstore.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limit for the authentication endpoints. Attempts are counted per client
 * address and path so brute-forcing one account cannot block registrations for everyone else.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/register");
    private static final long WINDOW_MS = 60_000;

    private record Window(long start, int count) {
    }

    private final ObjectMapper objectMapper;
    private final int limit;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(ObjectMapper objectMapper,
                               @Value("${app.security.auth-rate-limit-per-minute:10}") int limit) {
        this.objectMapper = objectMapper;
        this.limit = limit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        if (!tryAcquire(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED", "Too many attempts, please try again later", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};
        windows.compute(key, (k, current) -> {
            if (current == null || now - current.start() >= WINDOW_MS) {
                allowed[0] = true;
                return new Window(now, 1);
            }
            if (current.count() < limit) {
                allowed[0] = true;
                return new Window(current.start(), current.count() + 1);
            }
            return current;
        });
        return allowed[0];
    }
}
