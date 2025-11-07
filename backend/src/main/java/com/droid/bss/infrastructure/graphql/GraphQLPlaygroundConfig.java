package com.droid.bss.infrastructure.graphql;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.boot.GraphQlPlaygroundProperties;

/**
 * Configuration for GraphQL Playground (Development UI)
 */
@Configuration
@ConditionalOnProperty(value = "spring.graphql.playground.enabled", havingValue = "true", matchIfMissing = true)
public class GraphQLPlaygroundConfig {

    @Bean
    public GraphQlPlaygroundProperties graphQlPlaygroundProperties() {
        GraphQlPlaygroundProperties properties = new GraphQlPlaygroundProperties();

        // Enable playground in development
        properties.setEnabled(true);

        // Set settings
        properties.getSettings().put("graphQLIntrospection", true);
        properties.getSettings().put("requestCredentials", "same-origin");
        properties.getSettings().put("editor.fontSize", 14);
        properties.getSettings().put("editor.theme", "dark");
        properties.getSettings().put("editor.reuseHeaders", true);
        properties.getSettings().put("editor.fontFamily", "Monaco, Menlo, 'Ubuntu Mono', monospace");

        // Set headers
        properties.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        return properties;
    }
}
