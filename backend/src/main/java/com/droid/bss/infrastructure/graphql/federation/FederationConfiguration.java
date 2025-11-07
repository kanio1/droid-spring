package com.droid.bss.infrastructure.graphql.federation;

import com.apollographql.federation.graphqljava.FederationTransform;
import com.apollographql.federation.graphqljava.SchemaTransformer;
import com.apollographql.federation.graphqljava.tracing.FederatedTracingInstrumentation;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.validation.rules.ProvidedNonNullArgumentRule;
import graphql.validation.rules.ProvidedRequiredArgumentsRule;
import graphql.validation.rules.ValidatorRules;
import graphql.validation.validators.DocumentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.util.Set;

/**
 * GraphQL Federation Configuration
 * Configures Apollo Federation support for the BSS GraphQL API
 */
@Slf4j
@Configuration
public class FederationConfiguration {

    /**
     * Configure GraphQL Federation runtime wiring
     */
    @Bean
    public RuntimeWiringConfigurer federationRuntimeWiring(
            FederationEntityResolver entityResolver) {

        return wiringBuilder -> {
            var runtimeWiring = wiringBuilder.build();

            // Add custom scalar types for federation
            var newWiring = RuntimeWiring.newRuntimeWiring(runtimeWiring);

            // Add federation entity resolvers
            newWiring.type("Query", typeWiring -> typeWiring
                .dataFetcher("product", entityResolver.getProductResolver())
                .dataFetcher("customer", entityResolver.getCustomerResolver())
                .dataFetcher("order", entityResolver.getOrderResolver())
                .dataFetcher("invoice", entityResolver.getInvoiceResolver())
                .dataFetcher("payment", entityResolver.getPaymentResolver())
            );

            // Add subscriptions if needed
            newWiring.subscription("productUpdated", entityResolver.getProductSubscriptionResolver());

            // Add validation rules
            newWiring.directiveWiring(federationDirectiveWiring());

            log.info("GraphQL Federation runtime wiring configured");
        };
    }

    /**
     * Create federated GraphQL schema
     */
    @Bean
    public GraphQL federatedGraphQL(GraphQLSchema schema) {
        log.info("Creating federated GraphQL schema");

        return GraphQL.newGraphQL(schema)
            .queryValidationRules(DocumentValidator.defaultRules())
            .mutationValidationRules(DocumentValidator.defaultRules())
            .subscriptionValidationRules(DocumentValidator.defaultRules())
            .instrumentation(new FederatedTracingInstrumentation())
            .build();
    }

    /**
     * Federation directive wiring for custom directives
     */
    @Bean
    public FederationDirectiveWiring federationDirectiveWiring() {
        return new FederationDirectiveWiring();
    }

    /**
     * Entity resolver for federation
     */
    @Bean
    public FederationEntityResolver entityResolver() {
        return new FederationEntityResolver();
    }

    /**
     * Federation tracing configuration
     */
    @Bean
    public FederationTracingConfig federationTracingConfig() {
        return new FederationTracingConfig();
    }

    /**
     * Federation validation rules
     */
    @Bean
    public ValidatorRules federationValidatorRules() {
        return ValidatorRules.newValidatorRules()
            .addRule(new ProvidedNonNullArgumentRule())
            .addRule(new ProvidedRequiredArgumentsRule());
    }

    /**
     * Custom scalar types for federated schema
     */
    @Bean
    public GraphQLScalarType extendedDateTimeScalar() {
        return ExtendedScalars.DateTime;
    }

    @Bean
    public GraphQLScalarType extendedJSONScalar() {
        return ExtendedScalars.JSON;
    }

    @Bean
    public GraphQLScalarType extendedObjectScalar() {
        return ExtendedScalars.Object;
    }
}
