package com.droid.bss.infrastructure.database.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultShardManager}.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultShardManager")
class DefaultShardManagerTest {

    @Mock
    private ShardingStrategy mockShardingStrategy;

    private DefaultShardManager shardManager;

    @BeforeEach
    void setUp() {
        shardManager = new DefaultShardManager(mockShardingStrategy);
    }

    @Nested
    @DisplayName("Shard Registration")
    class ShardRegistrationTests {

        @Test
        @DisplayName("Should register shard successfully")
        void shouldRegisterShard() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");

            shardManager.registerShard(shard);

            Optional<Shard> retrieved = shardManager.getShard("shard1");
            assertTrue(retrieved.isPresent());
            assertEquals("shard1", retrieved.get().getId());
            assertEquals("shard1", retrieved.get().getName());
        }

        @Test
        @DisplayName("Should throw exception when registering null shard")
        void shouldThrowExceptionWhenRegisteringNullShard() {
            assertThrows(IllegalArgumentException.class, () -> shardManager.registerShard(null));
        }

        @Test
        @DisplayName("Should throw exception when registering shard with null ID")
        void shouldThrowExceptionWhenRegisteringShardWithNullId() {
            Shard shard = Shard.newBuilder()
                .name("test")
                .connectionUrl("jdbc:postgresql://localhost:5432/db1")
                .build();

            assertThrows(IllegalArgumentException.class, () -> shardManager.registerShard(shard));
        }

        @Test
        @DisplayName("Should unregister shard successfully")
        void shouldUnregisterShard() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            shardManager.registerShard(shard);

            shardManager.unregisterShard("shard1");

            Optional<Shard> retrieved = shardManager.getShard("shard1");
            assertFalse(retrieved.isPresent());
        }

        @Test
        @DisplayName("Should handle unregistering non-existent shard gracefully")
        void shouldHandleUnregisteringNonExistentShard() {
            assertDoesNotThrow(() -> shardManager.unregisterShard("non-existent"));
        }
    }

    @Nested
    @DisplayName("Shard Routing")
    class ShardRoutingTests {

        @Test
        @DisplayName("Should route to shard successfully")
        void shouldRouteToShard() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenReturn("shard1");
            when(mockShardingStrategy.getName()).thenReturn("HASH");

            shardManager.registerShard(shard);

            ShardKey shardKey = ShardKey.of("test-key");
            Optional<Shard> routed = shardManager.route(shardKey, ShardOperation.WRITE);

            assertTrue(routed.isPresent());
            assertEquals("shard1", routed.get().getId());
        }

        @Test
        @DisplayName("Should throw exception when routing with null shard key")
        void shouldThrowExceptionWhenRoutingWithNullShardKey() {
            assertThrows(IllegalArgumentException.class,
                () -> shardManager.route(null, ShardOperation.WRITE));
        }

        @Test
        @DisplayName("Should throw exception when routing with null operation")
        void shouldThrowExceptionWhenRoutingWithNullOperation() {
            ShardKey shardKey = ShardKey.of("test-key");
            assertThrows(IllegalArgumentException.class,
                () -> shardManager.route(shardKey, null));
        }

        @Test
        @DisplayName("Should return empty when no active shards available")
        void shouldReturnEmptyWhenNoActiveShards() {
            ShardKey shardKey = ShardKey.of("test-key");
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenReturn("non-existent");

            Optional<Shard> routed = shardManager.route(shardKey, ShardOperation.WRITE);

            assertFalse(routed.isPresent());
        }
    }

    @Nested
    @DisplayName("Broadcast Operations")
    class BroadcastTests {

        @Test
        @DisplayName("Should broadcast to all active shards for read operations")
        void shouldBroadcastToAllActiveShardsForRead() {
            Shard shard1 = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            Shard shard2 = createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2");

            shardManager.registerShard(shard1);
            shardManager.registerShard(shard2);

            AtomicInteger counter = new AtomicInteger(0);
            Runnable task = counter::incrementAndGet;

            shardManager.broadcast(task, ShardOperation.READ);

            assertEquals(2, counter.get());
        }

        @Test
        @DisplayName("Should broadcast to random shard for write operations")
        void shouldBroadcastToRandomShardForWrite() {
            Shard shard1 = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            Shard shard2 = createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2");

            shardManager.registerShard(shard1);
            shardManager.registerShard(shard2);

            AtomicInteger counter = new AtomicInteger(0);
            Runnable task = counter::incrementAndGet;

            shardManager.broadcast(task, ShardOperation.WRITE);

            assertEquals(1, counter.get());
        }

        @Test
        @DisplayName("Should use custom executor when provided")
        void shouldUseCustomExecutorWhenProvided() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            shardManager.registerShard(shard);

            Executor customExecutor = mock(Executor.class);
            Runnable task = mock(Runnable.class);

            shardManager.broadcast(task, ShardOperation.READ, customExecutor);

            verify(customExecutor, times(1)).execute(any(Runnable.class));
        }

        @Test
        @DisplayName("Should throw exception when broadcasting null task")
        void shouldThrowExceptionWhenBroadcastingNullTask() {
            assertThrows(IllegalArgumentException.class,
                () -> shardManager.broadcast(null, ShardOperation.READ));
        }

        @Test
        @DisplayName("Should throw exception when broadcasting with null operation")
        void shouldThrowExceptionWhenBroadcastingWithNullOperation() {
            Runnable task = mock(Runnable.class);
            assertThrows(IllegalArgumentException.class,
                () -> shardManager.broadcast(task, null));
        }

        @Test
        @DisplayName("Should wait for all tasks to complete")
        void shouldWaitForAllTasksToComplete() throws InterruptedException {
            Shard shard1 = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            Shard shard2 = createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2");

            shardManager.registerShard(shard1);
            shardManager.registerShard(shard2);

            CountDownLatch latch = new CountDownLatch(2);
            Runnable task = () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            };

            long start = System.currentTimeMillis();
            shardManager.broadcast(task, ShardOperation.READ);
            long elapsed = System.currentTimeMillis() - start;

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertTrue(elapsed >= 100);
            assertTrue(elapsed < 500);
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should track routing statistics")
        void shouldTrackRoutingStatistics() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenReturn("shard1");
            when(mockShardingStrategy.getName()).thenReturn("HASH");

            shardManager.registerShard(shard);

            ShardKey shardKey = ShardKey.of("test-key");
            shardManager.route(shardKey, ShardOperation.WRITE);

            ShardRoutingStats stats = shardManager.getRoutingStats();
            assertEquals(1, stats.getTotalRequests());
        }

        @Test
        @DisplayName("Should clear statistics")
        void shouldClearStatistics() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenReturn("shard1");
            when(mockShardingStrategy.getName()).thenReturn("HASH");

            shardManager.registerShard(shard);

            ShardKey shardKey = ShardKey.of("test-key");
            shardManager.route(shardKey, ShardOperation.WRITE);

            shardManager.clearStats();

            ShardRoutingStats stats = shardManager.getRoutingStats();
            assertEquals(0, stats.getTotalRequests());
        }

        @Test
        @DisplayName("Should calculate cache hit rate")
        void shouldCalculateCacheHitRate() {
            Shard shard = createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1");
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenReturn("shard1");
            when(mockShardingStrategy.getName()).thenReturn("HASH");

            shardManager.registerShard(shard);

            ShardKey shardKey = ShardKey.of("test-key");

            shardManager.route(shardKey, ShardOperation.WRITE);
            shardManager.route(shardKey, ShardOperation.WRITE);

            ShardRoutingStats stats = shardManager.getRoutingStats();
            assertTrue(stats.getCacheHitRate() > 50);
        }

        @Test
        @DisplayName("Should calculate error rate")
        void shouldCalculateErrorRate() {
            when(mockShardingStrategy.determineShard(any(ShardKey.class), anyList()))
                .thenThrow(new RuntimeException("Test error"));
            when(mockShardingStrategy.getName()).thenReturn("HASH");

            ShardKey shardKey = ShardKey.of("test-key");
            shardManager.route(shardKey, ShardOperation.WRITE);

            ShardRoutingStats stats = shardManager.getRoutingStats();
            assertTrue(stats.getErrorRate() > 0);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        @DisplayName("Should return correct shard count")
        void shouldReturnCorrectShardCount() {
            shardManager.registerShard(createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1"));
            shardManager.registerShard(createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2"));

            assertEquals(2, shardManager.getShardCount());
        }

        @Test
        @DisplayName("Should return correct active shard count")
        void shouldReturnCorrectActiveShardCount() {
            shardManager.registerShard(createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1"));
            shardManager.registerShard(createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2"));

            assertEquals(2, shardManager.getActiveShardCount());
        }

        @Test
        @DisplayName("Should return correct active shard count after status change")
        void shouldReturnCorrectActiveShardCountAfterStatusChange() {
            shardManager.registerShard(createTestShard("shard1", "jdbc:postgresql://localhost:5432/db1"));
            shardManager.registerShard(createTestShard("shard2", "jdbc:postgresql://localhost:5432/db2"));

            Shard shard1 = shardManager.getShard("shard1").orElseThrow();
            shardManager.registerShard(shard1.updateStatus(ShardStatus.UNAVAILABLE));

            assertEquals(1, shardManager.getActiveShardCount());
        }

        @Test
        @DisplayName("Should provide meaningful toString output")
        void shouldProvideMeaningfulToStringOutput() {
            when(mockShardingStrategy.getName()).thenReturn("HASH");
            when(mockShardingStrategy.getDescription()).thenReturn("Hash-based sharding");

            String output = shardManager.toString();

            assertNotNull(output);
            assertTrue(output.contains("HASH"));
        }
    }

    private Shard createTestShard(String id, String connectionUrl) {
        return Shard.newBuilder()
            .id(id)
            .name(id)
            .connectionUrl(connectionUrl)
            .build();
    }
}
