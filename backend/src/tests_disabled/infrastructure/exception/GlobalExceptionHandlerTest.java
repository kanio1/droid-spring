package com.droid.bss.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Infrastructure Layer")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("should handle AuthenticationException with 401 status")
    void shouldHandleAuthenticationExceptionWith401Status() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials") {};

        // When
        ProblemDetail result = exceptionHandler.handleAuthentication(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Authentication failed");
        assertThat(result.getDetail()).isEqualTo("Invalid credentials");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:security.authentication"));
        assertThat(result.getProperties()).containsKey("code");
        assertThat(result.getProperties()).containsKey("traceId");
        assertThat(result.getProperties().get("code")).isEqualTo("security.authentication");
        assertThat(result.getProperties().get("traceId")).isNotNull();
    }

    @Test
    @DisplayName("should handle AccessDeniedException with 403 status")
    void shouldHandleAccessDeniedExceptionWith403Status() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied to resource");

        // When
        ProblemDetail result = exceptionHandler.handleAccessDenied(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getTitle()).isEqualTo("Access denied");
        assertThat(result.getDetail()).isEqualTo("Access denied to resource");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:security.authorization"));
        assertThat(result.getProperties().get("code")).isEqualTo("security.authorization");
    }

    @Test
    @DisplayName("should handle IllegalArgumentException with 400 status")
    void shouldHandleIllegalArgumentExceptionWith400Status() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Customer not found");

        // When
        ProblemDetail result = exceptionHandler.handleIllegalArgument(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid argument");
        assertThat(result.getDetail()).isEqualTo("Customer not found");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:validation.illegalArgument"));
        assertThat(result.getProperties().get("code")).isEqualTo("validation.illegalArgument");
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException with validation errors")
    void shouldHandleMethodArgumentNotValidExceptionWithValidationErrors() {
        // Given
        MethodArgumentNotValidException exception = createValidationException();

        // When
        ProblemDetail result = exceptionHandler.handleValidation(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation failed");
        assertThat(result.getDetail()).isEqualTo("Request validation failed");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:validation.methodArgument"));
        assertThat(result.getProperties().get("code")).isEqualTo("validation.methodArgument");
        assertThat(result.getProperties()).containsKey("errors");
        
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors).contains("firstName: must not be blank", "email: must be a well-formed email address");
    }

    @Test
    @DisplayName("should handle IllegalStateException with 409 status")
    void shouldHandleIllegalStateExceptionWith409Status() {
        // Given
        IllegalStateException exception = new IllegalStateException("Customer already exists");

        // When
        ProblemDetail result = exceptionHandler.handleIllegalState(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Illegal state");
        assertThat(result.getDetail()).isEqualTo("Customer already exists");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:state.illegal"));
        assertThat(result.getProperties().get("code")).isEqualTo("state.illegal");
    }

    @Test
    @DisplayName("should handle RuntimeException with 500 status")
    void shouldHandleRuntimeExceptionWith500Status() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected runtime error");

        // When
        ProblemDetail result = exceptionHandler.handleRuntimeException(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal server error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:system.runtime"));
        assertThat(result.getProperties().get("code")).isEqualTo("system.runtime");
    }

    @Test
    @DisplayName("should handle generic Exception with 500 status")
    void shouldHandleGenericExceptionWith500Status() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ProblemDetail result = exceptionHandler.handleException(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal server error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(result.getType()).isEqualTo(URI.create("urn:problem:system.general"));
        assertThat(result.getProperties().get("code")).isEqualTo("system.general");
    }

    @Test
    @DisplayName("should generate unique trace IDs for different exceptions")
    void shouldGenerateUniqueTraceIdsForDifferentExceptions() {
        // Given
        IllegalArgumentException exception1 = new IllegalArgumentException("Error 1");
        IllegalArgumentException exception2 = new IllegalArgumentException("Error 2");

        // When
        ProblemDetail result1 = exceptionHandler.handleIllegalArgument(exception1);
        ProblemDetail result2 = exceptionHandler.handleIllegalArgument(exception2);

        // Then
        String traceId1 = (String) result1.getProperties().get("traceId");
        String traceId2 = (String) result2.getProperties().get("traceId");
        
        assertThat(traceId1).isNotNull();
        assertThat(traceId2).isNotNull();
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("should handle IllegalArgumentException with null message")
    void shouldHandleIllegalArgumentExceptionWithNullMessage() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException();

        // When
        ProblemDetail result = exceptionHandler.handleIllegalArgument(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid argument");
        assertThat(result.getDetail()).isEqualTo(null);
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException with no field errors")
    void shouldHandleMethodArgumentNotValidExceptionWithNoFieldErrors() {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult);

        // When
        ProblemDetail result = exceptionHandler.handleValidation(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getProperties().get("errors")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getProperties().get("errors");
        assertThat(errors).isEmpty();
    }

    private MethodArgumentNotValidException createValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "command");
        bindingResult.addError(new FieldError("command", "firstName", "must not be blank"));
        bindingResult.addError(new FieldError("command", "email", "must be a well-formed email address"));
        
        return new MethodArgumentNotValidException(null, bindingResult);
    }
}
