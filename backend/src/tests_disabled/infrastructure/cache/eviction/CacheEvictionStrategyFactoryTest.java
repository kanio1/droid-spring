package com.droid.bss.infrastructure.cache.eviction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for CacheEvictionStrategyFactory
 *
 * @since 1.0
 */
@DisplayName("CacheEvictionStrategyFactory Unit Tests")
class CacheEvictionStrategyFactoryTest {

    private CacheEvictionStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CacheEvictionStrategyFactory();
    }

    @Test
    @DisplayName("Should create LRU strategy")
    void shouldCreateLRUStrategy() {
        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createLRUStrategy();

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("LRU");
        assertThat(strategy.getPolicy()).isInstanceOf(LRUEvictionPolicy.class);
    }

    @Test
    @DisplayName("Should create LFU strategy")
    void shouldCreateLFUStrategy() {
        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createLFUStrategy();

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("LFU");
        assertThat(strategy.getPolicy()).isInstanceOf(LFUEvictionPolicy.class);
    }

    @Test
    @DisplayName("Should create TTL strategy")
    void shouldCreateTLTStrategy() {
        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createTLTStrategy(3600000);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("TTL");
        assertThat(strategy.getPolicy()).isInstanceOf(TTLEvictionPolicy.class);
    }

    @Test
    @DisplayName("Should create size-based strategy")
    void shouldCreateSizeBasedStrategy() {
        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createSizeBasedStrategy(1024000);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("SIZE");
        assertThat(strategy.getPolicy()).isInstanceOf(SizeBasedEvictionPolicy.class);
    }

    @Test
    @DisplayName("Should create combined LRU and TTL strategy")
    void shouldCreateCombinedLRUAndTLTStrategy() {
        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createLRUWithTLTStrategy(3600000);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("COMBINED");
    }

    @Test
    @DisplayName("Should create custom strategy")
    void shouldCreateCustomStrategy() {
        // Given
        EvictionPolicy<String, Object> policy = new LRUEvictionPolicy<>();

        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createCustomStrategy(policy);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getPolicy()).isEqualTo(policy);
    }

    @Test
    @DisplayName("Should create strategy by name")
    void shouldCreateStrategyByName() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ttlMs", 3600000L);

        // When
        CacheEvictionStrategy<String, Object> strategy = factory.createByName("LRU", parameters);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getName()).isEqualTo("LRU");
    }

    @Test
    @DisplayName("Should get available policies")
    void shouldGetAvailablePolicies() {
        // When
        String[] policies = factory.getAvailablePolicies();

        // Then
        assertThat(policies).isNotEmpty();
        assertThat(policies).contains("LRU", "LFU", "TTL", "SIZE");
    }

    @Test
    @DisplayName("Should throw exception for unknown policy name")
    void shouldThrowExceptionForUnknownPolicyName() {
        // Given
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> factory.createByName("UNKNOWN_POLICY", parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown policy");
    }

    @Test
    @DisplayName("Should throw exception for null policy name")
    void shouldThrowExceptionForNullPolicyName() {
        // When & Then
        assertThatThrownBy(() -> factory.createByName(null, new HashMap<>()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Policy name cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for TTL policy without ttlMs parameter")
    void shouldThrowExceptionForTTLPolicyWithoutTtlMs() {
        // Given
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> factory.createByName("TTL", parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TTL policy requires ttlMs parameter");
    }

    @Test
    @DisplayName("Should throw exception for SIZE policy without maxCacheSize parameter")
    void shouldThrowExceptionForSIZEPolicyWithoutMaxCacheSize() {
        // Given
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> factory.createByName("SIZE", parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SIZE policy requires maxCacheSize parameter");
    }
}
