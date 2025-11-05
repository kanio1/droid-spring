package com.droid.bss.infrastructure.database.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RangeShardingStrategy}.
 *
 * @since 1.0
 */
@DisplayName("RangeShardingStrategy")
class RangeShardingStrategyTest {

    private RangeShardingStrategy strategy;
    private List<Shard> shards;

    @BeforeEach
    void setUp() {
        strategy = new RangeShardingStrategy(0, 1000000);
        strategy.addRange(0, 333333, "shard1")
            .addRange(333333, 666666, "shard2")
            .addRange(666666, 1000000, "shard3");

        shards = List.of(
            Shard.newBuilder()
                .id("shard1")
                .name("Shard 1")
                .connectionUrl("jdbc:postgresql://localhost:5432/db1")
                .build(),
            Shard.newBuilder()
                .id("shard2")
                .name("Shard 2")
                .connectionUrl("jdbc:postgresql://localhost:5432/db2")
                .build(),
            Shard.newBuilder()
                .id("shard3")
                .name("Shard 3")
                .connectionUrl("jdbc:postgresql://localhost:5432/db3")
                .build()
        );
    }

    @Test
    @DisplayName("Should create strategy with valid range")
    void shouldCreateStrategyWithValidRange() {
        RangeShardingStrategy newStrategy = new RangeShardingStrategy(0, 1000);
        assertEquals(0, getPrivateField(newStrategy, "minValue"));
        assertEquals(1000, getPrivateField(newStrategy, "maxValue"));
    }

    @Test
    @DisplayName("Should throw exception when min value is not less than max value")
    void shouldThrowExceptionWhenMinValueIsNotLessThanMaxValue() {
        assertThrows(IllegalArgumentException.class, () -> new RangeShardingStrategy(1000, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RangeShardingStrategy(1000, 500));
    }

    @Test
    @DisplayName("Should add range successfully")
    void shouldAddRangeSuccessfully() {
        RangeShardingStrategy newStrategy = new RangeShardingStrategy(0, 1000);
        newStrategy.addRange(0, 500, "shard1");

        assertDoesNotThrow(() -> newStrategy.determineShard(ShardKey.of(250L), shards));
    }

    @Test
    @DisplayName("Should throw exception when range start is not less than end")
    void shouldThrowExceptionWhenRangeStartIsNotLessThanEnd() {
        assertThrows(IllegalArgumentException.class,
            () -> strategy.addRange(500, 500, "shard4"));
        assertThrows(IllegalArgumentException.class,
            () -> strategy.addRange(500, 400, "shard4"));
    }

    @Test
    @DisplayName("Should throw exception when range is outside strategy bounds")
    void shouldThrowExceptionWhenRangeIsOutsideStrategyBounds() {
        assertThrows(IllegalArgumentException.class,
            () -> strategy.addRange(-100, 100, "shard4"));
        assertThrows(IllegalArgumentException.class,
            () -> strategy.addRange(1000000, 1100000, "shard4"));
        assertThrows(IllegalArgumentException.class,
            () -> strategy.addRange(999999, 2000000, "shard4"));
    }

    @Test
    @DisplayName("Should determine correct shard for key in first range")
    void shouldDetermineCorrectShardForKeyInFirstRange() {
        ShardKey shardKey = ShardKey.of(100000L);

        String shardId = strategy.determineShard(shardKey, shards);

        assertEquals("shard1", shardId);
    }

    @Test
    @DisplayName("Should determine correct shard for key in second range")
    void shouldDetermineCorrectShardForKeyInSecondRange() {
        ShardKey shardKey = ShardKey.of(500000L);

        String shardId = strategy.determineShard(shardKey, shards);

        assertEquals("shard2", shardId);
    }

    @Test
    @DisplayName("Should determine correct shard for key in third range")
    void shouldDetermineCorrectShardForKeyInThirdRange() {
        ShardKey shardKey = ShardKey.of(800000L);

        String shardId = strategy.determineShard(shardKey, shards);

        assertEquals("shard3", shardId);
    }

    @Test
    @DisplayName("Should determine shard object for given key")
    void shouldDetermineShardObjectForGivenKey() {
        ShardKey shardKey = ShardKey.of(500000L);

        Shard shard = strategy.determineShardObject(shardKey, shards);

        assertEquals("shard2", shard.getId());
    }

    @Test
    @DisplayName("Should throw exception when shard key is null")
    void shouldThrowExceptionWhenShardKeyIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(null, shards));
    }

    @Test
    @DisplayName("Should throw exception when shards list is null")
    void shouldThrowExceptionWhenShardsListIsNull() {
        ShardKey shardKey = ShardKey.of(500000L);
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, null));
    }

    @Test
    @DisplayName("Should throw exception when shards list is empty")
    void shouldThrowExceptionWhenShardsListIsEmpty() {
        ShardKey shardKey = ShardKey.of(500000L);
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, List.of()));
    }

    @Test
    @DisplayName("Should throw exception when shard key is not numeric")
    void shouldThrowExceptionWhenShardKeyIsNotNumeric() {
        ShardKey shardKey = ShardKey.of("user123");
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, shards));
    }

    @Test
    @DisplayName("Should throw exception when key value is below range")
    void shouldThrowExceptionWhenKeyValueIsBelowRange() {
        ShardKey shardKey = ShardKey.of(-100L);
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, shards));
    }

    @Test
    @DisplayName("Should throw exception when key value is above range")
    void shouldThrowExceptionWhenKeyValueIsAboveRange() {
        ShardKey shardKey = ShardKey.of(2000000L);
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, shards));
    }

    @Test
    @DisplayName("Should throw exception when determined shard is inactive")
    void shouldThrowExceptionWhenDeterminedShardIsInactive() {
        List<Shard> inactiveShards = List.of(
            Shard.newBuilder()
                .id("shard1")
                .name("Shard 1")
                .connectionUrl("jdbc:postgresql://localhost:5432/db1")
                .status(ShardStatus.UNAVAILABLE)
                .build()
        );

        ShardKey shardKey = ShardKey.of(100000L);
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, inactiveShards));
    }

    @Test
    @DisplayName("Should return false for requiresHashing")
    void shouldReturnFalseForRequiresHashing() {
        assertFalse(strategy.requiresHashing());
    }

    @Test
    @DisplayName("Should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("RANGE", strategy.getName());
    }

    @Test
    @DisplayName("Should return correct strategy description")
    void shouldReturnCorrectStrategyDescription() {
        String description = strategy.getDescription();
        assertTrue(description.contains("RANGE"));
        assertTrue(description.contains("0"));
        assertTrue(description.contains("1000000"));
        assertTrue(description.contains("range(s)"));
    }

    @Test
    @DisplayName("Should provide meaningful toString output")
    void shouldProvideMeaningfulToStringOutput() {
        String output = strategy.toString();
        assertNotNull(output);
        assertTrue(output.contains("RangeShardingStrategy"));
        assertTrue(output.contains("RANGE"));
    }

    private int getPrivateField(RangeShardingStrategy strategy, String fieldName) {
        try {
            java.lang.reflect.Field field = RangeShardingStrategy.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (int) field.get(strategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
