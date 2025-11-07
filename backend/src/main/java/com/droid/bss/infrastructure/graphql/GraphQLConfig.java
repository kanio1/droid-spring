package com.droid.bss.infrastructure.graphql;

import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Configuration
 * Configures custom scalars, schema, and runtime wiring
 */
@Configuration
public class GraphQLConfig {

    /**
     * Configure runtime wiring with custom scalars and data fetchers
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            // Custom Scalars
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.BigDecimal)
            .scalar(ExtendedScalars.UUID)

            // Custom scalar for LocalDateTime
            .scalar("DateTime", builder -> builder
                .coercing(new LocalDateTimeCoercing())
            )
            .scalar("Date", builder -> builder
                .coercing(new LocalDateCoercing())
            )

            // DataFetcher for computed fields
            .type("Customer", typeWiring -> typeWiring
                .dataFetcher("totalRevenue", environment -> {
                    // This will be handled by the CustomerGraphQLController
                    return null;
                })
                .dataFetcher("activeSubscriptionsCount", environment -> {
                    return null;
                })
                .dataFetcher("overdueInvoicesCount", environment -> {
                    return null;
                })
                .dataFetcher("lastPaymentDate", environment -> {
                    return null;
                })
                .dataFetcher("customerSince", environment -> {
                    return null;
                })
                .dataFetcher("lifetimeValue", environment -> {
                    return null;
                })
                .dataFetcher("riskScore", environment -> {
                    return null;
                })
            )

            // Caching configuration
            .type("Query", typeWiring -> typeWiring
                .dataFetcher("customer", new CachingDataFetcher<>())
            )

            // Logging interceptor
            .build();
    }

    /**
     * GraphQL interceptors for logging and security
     */
    @Bean
    public List<WebGraphQlInterceptor> graphQlInterceptors() {
        return List.of(
            new QueryDepthLimitingInterceptor(),
            new LoggingInterceptor(),
            new SecurityInterceptor()
        );
    }

    /**
     * GraphQL Playground configuration
     */
    @Bean
    public GraphQLPlaygroundConfig playgroundConfig() {
        return new GraphQLPlaygroundConfig();
    }

    /**
     * Custom DataFetcher that provides caching
     */
    private static class CachingDataFetcher<T> implements graphql.schema.DataFetcher<CompletableFuture<T>> {
        @Override
        public CompletableFuture<T> get(graphql.schema.DataFetchingEnvironment environment) {
            // Implement caching logic here
            // For example, cache based on query and variables
            String cacheKey = createCacheKey(environment);
            return null; // Return cached or computed value
        }

        private String createCacheKey(graphql.schema.DataFetchingEnvironment environment) {
            return environment.getExecutionStepInfo().getPath().toString() +
                   environment.getArguments().toString();
        }
    }

    /**
     * Query Depth Limiting Interceptor
     * Prevents DoS attacks by limiting query depth
     */
    private static class QueryDepthLimitingInterceptor implements WebGraphQlInterceptor {
        private static final int MAX_QUERY_DEPTH = 10;

        @Override
        public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
            String query = request.getDocument();
            if (query != null) {
                int depth = calculateQueryDepth(query);
                if (depth > MAX_QUERY_DEPTH) {
                    log.warn("GraphQL Query too deep: {} (max allowed: {}) for operation: {}",
                        depth, MAX_QUERY_DEPTH, request.getOperationName().orElse("anonymous"));
                    return Mono.error(new QueryTooDeepException(
                        "Query depth " + depth + " exceeds maximum allowed depth of " + MAX_QUERY_DEPTH));
                }
                log.debug("GraphQL Query depth: {} (max: {})", depth, MAX_QUERY_DEPTH);
            }
            return chain.next(request);
        }

        private int calculateQueryDepth(String query) {
            int maxDepth = 0;
            int currentDepth = 0;
            int braceDepth = 0;

            for (int i = 0; i < query.length(); i++) {
                char c = query.charAt(i);
                if (c == '{') {
                    braceDepth++;
                    currentDepth = Math.max(currentDepth, braceDepth);
                } else if (c == '}') {
                    braceDepth--;
                }
            }
            maxDepth = currentDepth;
            return maxDepth;
        }
    }

    /**
     * Logging Interceptor
     */
    private static class LoggingInterceptor implements WebGraphQlInterceptor {
        @Override
        public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
            long startTime = System.currentTimeMillis();

            log.info("GraphQL Request - Operation: {}, Query: {}",
                request.getOperationName().orElse("anonymous"),
                sanitizeQuery(request.getDocument()));

            return chain.next(request)
                .doOnNext(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("GraphQL Response - Duration: {}ms, Path: {}",
                        duration, request.getUri().getPath());
                })
                .doOnError(error -> {
                    log.error("GraphQL Error: {}", error.getMessage(), error);
                });
        }

        private String sanitizeQuery(String query) {
            if (query == null) return "";
            // Remove newlines and limit length for logging
            return query.replace("\n", " ").substring(0, Math.min(200, query.length()));
        }
    }

    /**
     * Security Interceptor
     */
    private static class SecurityInterceptor implements WebGraphQlInterceptor {
        @Override
        public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
            // Add security checks here
            // For example, validate JWT token, check permissions, etc.

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new SecurityException("Missing or invalid authorization header");
            }

            // TODO: Validate JWT token and extract user context
            // request.putAttribute("userId", extractUserIdFromToken(authHeader));

            return chain.next(request);
        }
    }

    /**
     * LocalDateTime Coercing
     */
    private static class LocalDateTimeCoercing extends graphql.scalars.datetime.DateTimeCoercing {
        @Override
        public LocalDateTime parseValue(Object input) {
            return super.parseValue(input);
        }

        @Override
        public LocalDateTime parseLiteral(Object input) {
            return super.parseLiteral(input);
        }
    }

    /**
     * LocalDate Coercing
     */
    private static class LocalDateCoercing extends graphql.scalars.datetime.DateCoercing {
        @Override
        public LocalDate parseValue(Object input) {
            return super.parseValue(input);
        }

        @Override
        public LocalDate parseLiteral(Object input) {
            return super.parseLiteral(input);
        }
    }
}
