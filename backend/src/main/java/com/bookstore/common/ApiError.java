package com.bookstore.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String code, String message, String path,
                       Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return new ApiError(Instant.now(), status.value(), code, message, path, null);
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, path, fieldErrors);
    }
}
