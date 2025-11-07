package com.droid.bss.infrastructure.database.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HashShardingStrategy}.
 *
 * @since 1.0
 */
@DisplayName("HashShardingStrategy")
class HashShardingStrategyTest {

    private HashShardingStrategy strategy;
    private List<Shard> shards;

    @BeforeEach
    void setUp() {
        strategy = new HashShardingStrategy(31);
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
    @DisplayName("Should create strategy with custom hash multiplier")
    void shouldCreateStrategyWithCustomHashMultiplier() {
        HashShardingStrategy customStrategy = new HashShardingStrategy(17);
        assertEquals(17, getPrivateField(customStrategy, "hashMultiplier"));
    }

    @Test
    @DisplayName("Should create strategy with default hash multiplier")
    void shouldCreateStrategyWithDefaultHashMultiplier() {
        HashShardingStrategy defaultStrategy = new HashShardingStrategy();
        assertEquals(31, getPrivateField(defaultStrategy, "hashMultiplier"));
    }

    @Test
    @DisplayName("Should throw exception when hash multiplier is not positive")
    void shouldThrowExceptionWhenHashMultiplierIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new HashShardingStrategy(0));
        assertThrows(IllegalArgumentException.class, () -> new HashShardingStrategy(-1));
    }

    @Test
    @DisplayName("Should determine shard for given key")
    void shouldDetermineShardForGivenKey() {
        ShardKey shardKey = ShardKey.of("user123");

        String shardId = strategy.determineShard(shardKey, shards);

        assertNotNull(shardId);
        assertTrue(shardId.equals("shard1") || shardId.equals("shard2") || shardId.equals("shard3"));
    }

    @Test
    @DisplayName("Should determine shard object for given key")
    void shouldDetermineShardObjectForGivenKey() {
        ShardKey shardKey = ShardKey.of("user123");

        Shard shard = strategy.determineShardObject(shardKey, shards);

        assertNotNull(shard);
        assertTrue(shard.getId().equals("shard1") || shard.getId().equals("shard2") || shard.getId().equals("shard3"));
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
        ShardKey shardKey = ShardKey.of("user123");
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, null));
    }

    @Test
    @DisplayName("Should throw exception when shards list is empty")
    void shouldThrowExceptionWhenShardsListIsEmpty() {
        ShardKey shardKey = ShardKey.of("user123");
        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, List.of()));
    }

    @Test
    @DisplayName("Should use only active shards for routing")
    void shouldUseOnlyActiveShardsForRouting() {
        List<Shard> inactiveShards = List.of(
            Shard.newBuilder()
                .id("inactive1")
                .name("Inactive 1")
                .connectionUrl("jdbc:postgresql://localhost:5432/dbinactive1")
                .status(ShardStatus.UNAVAILABLE)
                .build(),
            Shard.newBuilder()
                .id("inactive2")
                .name("Inactive 2")
                .connectionUrl("jdbc:postgresql://localhost:5432/dbinactive2")
                .status(ShardStatus.DRAINING)
                .build()
        );

        ShardKey shardKey = ShardKey.of("user123");

        assertThrows(IllegalArgumentException.class,
            () -> strategy.determineShard(shardKey, inactiveShards));
    }

    @Test
    @DisplayName("Should return consistent shard for same key")
    void shouldReturnConsistentShardForSameKey() {
        ShardKey shardKey = ShardKey.of("user123");

        String shardId1 = strategy.determineShard(shardKey, shards);
        String shardId2 = strategy.determineShard(shardKey, shards);

        assertEquals(shardId1, shardId2);
    }

    @Test
    @DisplayName("Should distribute keys across shards")
    void shouldDistributeKeysAcrossShards() {
        int[] shardCounts = {0, 0, 0};

        for (int i = 0; i < 300; i++) {
            ShardKey shardKey = ShardKey.of("user" + i);
            String shardId = strategy.determineShard(shardKey, shards);

            int index = Integer.parseInt(shardId.substring(5)) - 1;
            shardCounts[index]++;
        }

        assertTrue(shardCounts[0] > 80);
        assertTrue(shardCounts[1] > 80);
        assertTrue(shardCounts[2] > 80);
    }

    @Test
    @DisplayName("Should return that hashing is required")
    void shouldReturnThatHashingIsRequired() {
        assertTrue(strategy.requiresHashing());
    }

    @Test
    @DisplayName("Should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("HASH", strategy.getName());
    }

    @Test
    @DisplayName("Should return correct strategy description")
    void shouldReturnCorrectStrategyDescription() {
        String description = strategy.getDescription();
        assertTrue(description.contains("HASH"));
        assertTrue(description.contains("31"));
    }

    @Test
    @DisplayName("Should provide meaningful toString output")
    void shouldProvideMeaningfulToStringOutput() {
        String output = strategy.toString();
        assertNotNull(output);
        assertTrue(output.contains("HashShardingStrategy"));
        assertTrue(output.contains("HASH"));
    }

    private int getPrivateField(HashShardingStrategy strategy, String fieldName) {
        try {
            java.lang.reflect.Field field = HashShardingStrategy.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (int) field.get(strategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
