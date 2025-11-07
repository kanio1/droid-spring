package com.droid.bss.infrastructure.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Gateway Configuration Properties
 */
@Data
@ConfigurationProperties(prefix = "bss.gateway")
@Validated
public class GatewayProperties {

    private RateLimit rateLimit = new RateLimit();

    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    private LoadBalancer loadBalancer = new LoadBalancer();

    private Security security = new Security();

    private Monitoring monitoring = new Monitoring();

    @Data
    public static class RateLimit {
        @Min(1)
        @Max(10000)
        private int defaultRate = 100;

        @Min(1)
        @Max(50000)
        private int burstCapacity = 200;

        @Min(1)
        @Max(100000)
        private int requestedTokens = 1000;

        private boolean enableGlobalRateLimit = true;

        private String globalRateLimitKey = "global";

        @Min(1)
        @Max(60)
        private int requestTimeout = 5;
    }

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;

        @Min(1)
        @Max(100)
        private int failureRateThreshold = 50;

        @Min(1)
        @Max(60)
        private int waitDurationInOpenState = 30;

        @Min(1)
        @Max(100)
        private int slidingWindowSize = 10;

        @Min(1)
        @Max(100)
        private int minimumNumberOfCalls = 5;

        @Min(1)
        @Max(10)
        private int slowCallDurationThreshold = 2;

        @Min(1)
        @Max(100)
        private int slowCallRateThreshold = 100;

        private boolean recordFailureException = true;
    }

    @Data
    public static class LoadBalancer {
        private String strategy = "round-robin"; // round-robin, random, sticky

        @Min(1)
        @Max(100)
        private int maxRetries = 3;

        @Min(100)
        @Max(10000)
        private int connectTimeout = 2000;

        @Min(100)
        @Max(30000)
        private int readTimeout = 5000;

        @Min(1)
        @Max(10)
        private int retryAttempts = 3;

        @Min(100)
        @Max(5000)
        private int retryDelay = 200;
    }

    @Data
    public static class Security {
        private boolean enableJwtValidation = true;

        private boolean enableRateLimiting = true;

        private boolean enableCors = true;

        private boolean enableHttpsRedirect = false;

        private boolean enableSecurityHeaders = true;

        private String[] allowedOrigins = {"*"};

        private String[] allowedMethods = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        };

        private String[] allowedHeaders = {"*"};

        private int maxAge = 3600;
    }

    @Data
    public static class Monitoring {
        private boolean enableMetrics = true;

        private boolean enableTracing = true;

        private boolean enableLogging = true;

        private String metricsPrefix = "bss.gateway";

        private int logLevel = 1; // 0=TRACE, 1=DEBUG, 2=INFO, 3=WARN, 4=ERROR
    }
}
