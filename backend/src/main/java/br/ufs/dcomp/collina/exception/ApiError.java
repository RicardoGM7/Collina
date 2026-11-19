package br.ufs.dcomp.collina.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public ApiError(Integer status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ApiError(Integer status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(Instant.now(), status, error, message, path, fieldErrors);
    }
}
