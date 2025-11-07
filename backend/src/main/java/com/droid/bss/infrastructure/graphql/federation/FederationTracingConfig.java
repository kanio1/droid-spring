package com.droid.bss.infrastructure.graphql.federation;

import com.apollographql.federation.graphqljava.tracing.FederatedTracingInstrumentation;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Federation Tracing Configuration
 * Configures tracing for federated GraphQL operations
 */
@Slf4j
@Configuration
public class FederationTracingConfig {

    @Bean
    public FederatedTracingInstrumentation federatedTracingInstrumentation() {
        log.info("Initializing Federated Tracing Instrumentation");

        return new FederatedTracingInstrumentation(
            // Configuration for tracing
            FederatedTracingInstrumentation.Options.builder()
                .captureSignature(true)
                .build()
        );
    }
}
