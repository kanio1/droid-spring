package com.droid.bss.infrastructure.exception;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detail.setTitle("Authentication failed");
        detail.setDetail(ex.getMessage());
        applyDefaults(detail, "security.authentication");
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        detail.setTitle("Access denied");
        detail.setDetail(ex.getMessage());
        applyDefaults(detail, "security.authorization");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Invalid argument");
        detail.setDetail(ex.getMessage());
        applyDefaults(detail, "validation.illegalArgument");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        detail.setDetail("Request validation failed");
        applyDefaults(detail, "validation.methodArgument");
        detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return detail;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("Illegal state");
        detail.setDetail(ex.getMessage());
        applyDefaults(detail, "state.illegal");
        return detail;
    }

    @ExceptionHandler(RuntimeException.class)
    ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("Internal server error");
        detail.setDetail("An unexpected error occurred");
        applyDefaults(detail, "system.runtime");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleException(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("Internal server error");
        detail.setDetail("An unexpected error occurred");
        applyDefaults(detail, "system.general");
        return detail;
    }

    private void applyDefaults(ProblemDetail detail, String code) {
        detail.setType(URI.create("urn:problem:%s".formatted(code)));
        detail.setProperty("code", code);
        detail.setProperty("traceId", UUID.randomUUID().toString());
    }
}
