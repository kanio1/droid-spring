package com.droid.bss.infrastructure.graphql.persisted;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persisted Query Configuration
 * Configures persisted query storage and processing
 */
@Slf4j
@Configuration
public class PersistedQueryConfiguration {

    /**
     * Create a persisted query storage
     */
    @Bean
    public PersistedQueryStorage persistedQueryStorage() {
        return new InMemoryPersistedQueryStorage();
    }

    /**
     * Create a persisted query hash generator
     */
    @Bean
    public PersistedQueryHashGenerator hashGenerator() {
        return new PersistedQueryHashGenerator();
    }

    /**
     * Create a persisted query validator
     */
    @Bean
    public PersistedQueryValidator queryValidator() {
        return new PersistedQueryValidator();
    }

    /**
     * Create a persisted query service
     */
    @Bean
    public PersistedQueryService persistedQueryService(
            PersistedQueryStorage storage,
            PersistedQueryHashGenerator hashGenerator,
            PersistedQueryValidator validator) {
        return new PersistedQueryService(storage, hashGenerator, validator);
    }
}
