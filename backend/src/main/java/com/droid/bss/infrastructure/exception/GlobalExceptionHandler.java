package com.droid.bss.infrastructure.exception;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

    private void applyDefaults(ProblemDetail detail, String code) {
        detail.setType(URI.create("urn:problem:%s".formatted(code)));
        detail.setProperty("code", code);
        detail.setProperty("traceId", UUID.randomUUID().toString());
    }
}
