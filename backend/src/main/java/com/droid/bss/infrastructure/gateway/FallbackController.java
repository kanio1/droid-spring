package com.droid.bss.infrastructure.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller
 * Provides fallback responses when services are unavailable
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/backend")
    public Mono<ResponseEntity<Map<String, Object>>> backendFallback() {
        log.warn("Backend service fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", "SERVICE_UNAVAILABLE");
        response.put("message", "Backend service is currently unavailable. Please try again later.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("retryAfter", 30); // seconds

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "30")
            .body(response));
    }

    @GetMapping("/graphql")
    public Mono<ResponseEntity<Map<String, Object>>> graphqlFallback() {
        log.warn("GraphQL service fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("errors", new Object[]{
            Map.of(
                "message", "GraphQL service is currently unavailable",
                "extensions", Map.of(
                    "code", "SERVICE_UNAVAILABLE",
                    "timestamp", LocalDateTime.now()
                )
            )
        });

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response));
    }

    @GetMapping("/rsocket")
    public Mono<ResponseEntity<Map<String, Object>>> rsocketFallback() {
        log.warn("RSocket service fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "unavailable");
        response.put("message", "RSocket service is currently unavailable");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("alternative", "Please use HTTP endpoints for now");

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "DOWN");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("gateway", "available");
        response.put("backend", "unavailable");
        response.put("components", Map.of(
            "gateway", Map.of("status", "UP"),
            "backend", Map.of("status", "DOWN", "error", "Service unavailable")
        ));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
