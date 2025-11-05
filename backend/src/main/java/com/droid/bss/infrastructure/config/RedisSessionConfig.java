package com.droid.bss.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.redis.RedisSessionRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for Redis-based session management
 * Enables distributed session storage for Keycloak and Spring applications
 */
@Configuration
@EnableSpringHttpSession
public class RedisSessionConfig {

    /**
     * Custom RedisTemplate for session storage
     */
    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JDK serialization for values (compatible with Spring Session)
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();
        template.setValueSerializer(jdkSerializer);
        template.setHashValueSerializer(jdkSerializer);

        template.setDefaultSerializer(jdkSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Executor for session cleanup tasks
     */
    @Bean
    public Executor sessionCleanupExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "session-cleanup");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Configure RedisSessionRepository with custom settings
     */
    @Bean
    public RedisSessionRepository sessionRepository(RedisConnectionFactory connectionFactory) {
        RedisSessionRepository repository = new RedisSessionRepository(connectionFactory);
        // Set session expiration to 30 minutes (1800 seconds)
        repository.setDefaultMaxInactiveInterval(1800);
        return repository;
    }
}
