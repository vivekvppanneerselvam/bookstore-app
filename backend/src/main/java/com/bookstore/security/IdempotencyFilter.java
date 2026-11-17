package com.bookstore.security;

import com.bookstore.common.ApiError;
import com.bookstore.common.Hashing;
import com.bookstore.model.IdempotencyRecord;
import com.bookstore.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Makes POST cart/order requests replay-safe: the first request with a given Idempotency-Key is
 * executed and its response stored, and repeats of the same key return that stored response.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_POST_PATHS = Set.of("/api/cart/items", "/api/orders");
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyRecordRepository records, ObjectMapper objectMapper) {
        this.records = records;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_POST_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthPrincipal auth)) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader("Idempotency-Key");
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            reject(request, response, HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "An Idempotency-Key header (8-64 letters, digits, '-' or '_') is required");
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        if (body.length > MAX_BODY_BYTES) {
            reject(request, response, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_TOO_LARGE", "Request body is too large");
            return;
        }

        String requestHash = Hashing.sha256(request.getMethod() + "|" + request.getRequestURI() + "|"
                + auth.id() + "|" + Hashing.sha256(body));

        Optional<IdempotencyRecord> existing = records.findByUserIdAndKey(auth.id(), key);
        if (existing.isPresent()) {
            replayOrReject(existing.get(), requestHash, request, response);
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord(auth.id(), key, requestHash);
        try {
            records.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            reject(request, response, HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                    "A request with this Idempotency-Key is already being processed");
            return;
        }

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyRequest(request, body), cachedResponse);
            int status = cachedResponse.getStatus();
            if (status < 500) {
                record.complete(status, new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
                records.save(record);
            } else {
                records.delete(record);
            }
        } catch (Exception ex) {
            records.delete(record);
            throw ex;
        } finally {
            cachedResponse.copyBodyToResponse();
        }
    }

    private void replayOrReject(IdempotencyRecord record, String requestHash, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        if (!Hashing.matches(record.getRequestHash(), requestHash)) {
            reject(request, response, HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was already used with a different request payload");
            return;
        }
        if (record.getStatus() != IdempotencyRecord.Status.COMPLETED) {
            reject(request, response, HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                    "A request with this Idempotency-Key is already being processed");
            return;
        }
        response.setStatus(record.getResponseStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Idempotency-Replayed", "true");
        response.getWriter().write(record.getResponseBody());
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code,
                        String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, code, message, request.getRequestURI()));
    }

    /** Lets the request body be read twice: once for hashing, once by the controller. */
    static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Async reads are not supported");
                }

                @Override
                public int read() {
                    return source.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return source.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
