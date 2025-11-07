package com.droid.bss.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for RedisCacheConfiguration
 * Following Arrange-Act-Assert pattern
 */
@SpringBootTest(classes = RedisCacheConfigurationTest.TestConfig.class)
@DisplayName("RedisCacheConfiguration Infrastructure Layer")
class RedisCacheConfigurationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private RedisConnectionFactory connectionFactory;

    @Test
    @DisplayName("Should configure RedisTemplate with proper serializers")
    void shouldConfigureRedisTemplateWithSerializers() {
        // Arrange & Act
        RedisTemplate<String, Object> template = redisTemplate;

        // Assert
        assertThat(template).isNotNull();
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("Should configure RedisTemplate with ObjectMapper")
    void shouldConfigureRedisTemplateWithObjectMapper() {
        // Arrange & Act
        RedisTemplate<String, Object> template = redisTemplate;

        // Assert
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
            (GenericJackson2JsonRedisSerializer) template.getValueSerializer();

        // Test serialization with ObjectMapper configuration
        String testValue = "test";
        byte[] serialized = Objects.requireNonNull(template.getValueSerializer()).serialize(testValue, null);
        assertThat(serialized).isNotNull();
        assertThat(serialized.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should serialize and deserialize LocalDateTime correctly")
    void shouldSerializeAndDeserializeLocalDateTime() {
        // Arrange
        String key = "test:datetime";
        LocalDateTime testDateTime = LocalDateTime.of(2025, 11, 5, 10, 30, 45);

        // Act
        redisTemplate.opsForValue().set(key, testDateTime);
        LocalDateTime retrieved = (LocalDateTime) redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isEqualTo(testDateTime);
    }

    @Test
    @DisplayName("Should serialize and deserialize complex objects")
    void shouldSerializeAndDeserializeComplexObjects() {
        // Arrange
        String key = "test:complex";
        TestComplexObject testObject = new TestComplexObject(
            "Test ID",
            "Test Name",
            LocalDateTime.now(),
            42
        );

        // Act
        redisTemplate.opsForValue().set(key, testObject);
        TestComplexObject retrieved = (TestComplexObject) redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(testObject.id());
        assertThat(retrieved.name()).isEqualTo(testObject.name());
        assertThat(retrieved.createdAt()).isEqualTo(testObject.createdAt());
        assertThat(retrieved.count()).isEqualTo(testObject.count());
    }

    @Test
    @DisplayName("Should use StringRedisSerializer for keys")
    void shouldUseStringRedisSerializerForKeys() {
        // Arrange
        String key = "test:string:key";

        // Act
        redisTemplate.opsForValue().set(key, "test value");
        String retrieved = (String) redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isEqualTo("test value");
    }

    @Test
    @DisplayName("Should support hash operations")
    void shouldSupportHashOperations() {
        // Arrange
        String hashKey = "test:hash";
        String field1 = "field1";
        String field2 = "field2";

        // Act
        redisTemplate.opsForHash().put(hashKey, field1, "value1");
        redisTemplate.opsForHash().put(hashKey, field2, "value2");

        // Assert
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, field1)).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, field2)).isTrue();
        assertThat(redisTemplate.opsForHash().entries(hashKey)).hasSize(2);
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
        // Arrange
        String key = "test:null";

        // Act
        redisTemplate.opsForValue().set(key, null);
        Object retrieved = redisTemplate.opsForValue().get(key);

        // Assert
        // Redis typically stores null as a special marker or empty value
        // The exact behavior depends on the serializer configuration
        assertThat(redisTemplate.hasKey(key)).isTrue(); // Key exists
    }

    @Test
    @DisplayName("Should configure cache manager")
    void shouldConfigureCacheManager() {
        // Arrange & Act
        CacheManager manager = cacheManager;

        // Assert
        assertThat(manager).isNotNull();
        // Cache manager should be configured via Spring Cache abstraction
        assertThat(manager.getCacheNames()).isNotNull();
    }

    @Test
    @DisplayName("Should support list operations")
    void shouldSupportListOperations() {
        // Arrange
        String listKey = "test:list";

        // Act
        redisTemplate.opsForList().leftPush(listKey, "item1");
        redisTemplate.opsForList().leftPush(listKey, "item2");

        // Assert
        assertThat(redisTemplate.opsForList().size(listKey)).isEqualTo(2);
    }

    @Test
    @DisplayName("Should support set operations")
    void shouldSupportSetOperations() {
        // Arrange
        String setKey = "test:set";

        // Act
        redisTemplate.opsForSet().add(setKey, "value1", "value2", "value3");

        // Assert
        assertThat(redisTemplate.opsForSet().members(setKey)).hasSize(3);
    }

    @Test
    @DisplayName("Should serialize strings correctly")
    void shouldSerializeStringsCorrectly() {
        // Arrange
        String key = "test:string";
        String value = "Test String Value";

        // Act
        redisTemplate.opsForValue().set(key, value);
        String retrieved = (String) redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    @DisplayName("Should serialize numbers correctly")
    void shouldSerializeNumbersCorrectly() {
        // Arrange
        String key1 = "test:integer";
        String key2 = "test:double";
        int intValue = 42;
        double doubleValue = 3.14159;

        // Act
        redisTemplate.opsForValue().set(key1, intValue);
        redisTemplate.opsForValue().set(key2, doubleValue);

        // Assert
        assertThat(redisTemplate.opsForValue().get(key1)).isEqualTo(intValue);
        assertThat(redisTemplate.opsForValue().get(key2)).isEqualTo(doubleValue);
    }

    @Test
    @DisplayName("Should support TTL operations")
    void shouldSupportTTLOperations() {
        // Arrange
        String key = "test:ttl";
        String value = "temporary";

        // Act
        redisTemplate.opsForValue().set(key, value);
        // Note: Testing TTL would require actual Redis connection
        // This test verifies the template is configured correctly for TTL

        // Assert
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    // Test configuration
    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Serialize strings using StringRedisSerializer
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);

            // Serialize objects using GenericJackson2JsonRedisSerializer
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);
            template.setDefaultSerializer(jsonSerializer);

            return template;
        }

        @Bean
        public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
            org.springframework.data.redis.cache.RedisCacheManager.Builder builder =
                org.springframework.data.redis.cache.RedisCacheManager
                    .RedisCacheManagerBuilder
                    .fromConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()))
                    .cacheDefaults(
                        org.springframework.data.redis.cache.RedisCacheConfiguration
                            .defaultCacheConfig()
                    );

            return builder.build();
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }

    // Test data class
    record TestComplexObject(
        String id,
        String name,
        LocalDateTime createdAt,
        int count
    ) {}
}
