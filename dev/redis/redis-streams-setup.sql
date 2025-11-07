-- Redis Streams Setup for CloudEvents
-- Target: High-throughput event streaming (50k+ msgs/sec)

-- ============================================================================
-- STREAM SETUP
-- ============================================================================

-- Create main events stream
-- XADD events:stream MAXLEN ~ 1000000 * event-type event-type payment.processed event-id evt-12345
-- Key: events:stream
-- Purpose: Main CloudEvents stream for all event types

-- Create tenant-specific streams
-- XADD events:tenant-001 MAXLEN ~ 100000 * event-type payment.processed event-id evt-12345
-- XADD events:tenant-002 MAXLEN ~ 1000000 * event-type order.created event-id evt-67890
-- Purpose: Per-tenant event streams for isolation

-- ============================================================================
-- CONSUMER GROUPS
-- ============================================================================

-- Create consumer groups for horizontal scaling
-- Each group can have multiple consumers for parallel processing

-- Main consumer groups
-- XGROUP CREATE events:stream payment-service $0 MKSTREAM
-- XGROUP CREATE events:stream order-service $0 MKSTREAM
-- XGROUP CREATE events:stream billing-service $0 MKSTREAM
-- XGROUP CREATE events:stream fraud-service $0 MKSTREAM

-- Tenant-specific consumer groups
-- XGROUP CREATE events:tenant-001 tenant-001-service $0 MKSTREAM
-- XGROUP CREATE events:tenant-002 tenant-002-service $0 MKSTREAM

-- ============================================================================
-- STREAMS CONFIGURATION
-- ============================================================================

-- Stream: events:stream
--   Purpose: Global CloudEvents stream
--   Maxlen: 1,000,000 entries
--   Consumer Groups: payment-service, order-service, billing-service, fraud-service
--   Rate: Target 6,667 events/sec

-- Stream: events:tenant-{tenant_id}
--   Purpose: Per-tenant event isolation
--   Maxlen: 1,000,000 entries per tenant
--   Consumer Groups: tenant-{id}-service
--   Rate: 133 events/sec per tenant (50 tenants)

-- ============================================================================
-- CONSUMER GROUP COMMANDS
-- ============================================================================

-- Read from consumer group (blocking)
-- XREADGROUP GROUP payment-service consumer-1 BLOCK 5000 STREAMS events:stream >

-- Read from consumer group (non-blocking)
-- XREADGROUP GROUP payment-service consumer-1 COUNT 100 STREAMS events:stream >

-- Acknowledge processed messages
-- XACK events:stream payment-service 1526984818136-0 1526984818136-1

-- View pending messages
-- XREADGROUP GROUP payment-service consumer-1

-- View consumer lag
-- XINFO GROUP payment-service events:stream

-- ============================================================================
-- STREAMS WITH TTL
-- ============================================================================

-- Add event with TTL (events expire automatically)
-- XADD events:ttl-stream MAXLEN ~ 100000 TTL 3600 * event-type payment.processed event-id evt-12345
-- Purpose: Events that expire after 1 hour (3600 seconds)

-- ============================================================================
-- REDIS STREAMS PATTERNS
-- ============================================================================

-- Pattern 1: Event Broadcasting
--   Producer: XADD events:stream * event-type payment.processed ...
--   Consumers: Multiple consumer groups subscribe to same stream
--   Use case: Fan-out pattern for event distribution

-- Pattern 2: Event Partitioning
--   Producer: XADD events:tenant-001 * event-type ... (for tenant 001)
--   Producer: XADD events:tenant-002 * event-type ... (for tenant 002)
--   Consumers: Separate consumer group per tenant
--   Use case: Tenant isolation and parallel processing

-- Pattern 3: Event Aggregation
--   Producer: XADD events:hourly-aggregates * event-type payment.sum ...
--   Consumers: Single consumer group processes aggregates
--   Use case: Event aggregation and summarization

-- Pattern 4: Event Partitioning by Type
--   Producer: XADD events:payment * event-type payment.processed ...
--   Producer: XADD events:order * event-type order.created ...
--   Consumers: Specialized consumer groups per event type
--   Use case: Type-based event routing

-- ============================================================================
-- PERFORMANCE TUNING
-- ============================================================================

-- Batch operations using PIPELINE
-- Example in Go:
--   pipe := redisClient.Pipeline()
--   for i := 0; i < 1000; i++ {
--       pipe.XAdd(ctx, &redis.XAddArgs{
--           Stream: "events:stream",
--           Values: map[string]interface{}{
--               "event-type": "payment.processed",
--               "event-id":   fmt.Sprintf("evt-%d", i),
--           },
--       })
--   }
--   pipe.Exec(ctx)

-- ============================================================================
-- MONITORING
-- ============================================================================

-- Check stream length
-- XLEN events:stream

-- Check consumer group info
-- XINFO GROUPS events:stream
-- XINFO CONSUMERS events:stream payment-service

-- Get stream info
-- XINFO STREAM events:stream

-- Monitor stream in real-time
-- XREAD BLOCK 0 STREAMS events:stream $

-- ============================================================================
-- REDIS STREAMS VS KAFKA
-- ============================================================================

-- Advantages of Redis Streams:
--   - Lower latency (sub-millisecond)
--   - Simpler setup (no ZooKeeper)
--   - Built-in persistence (AOF)
--   - Automatic expiration (MAXLEN)
--   - In-memory speed

-- Disadvantages vs Kafka:
--   - Limited to single Redis instance (or cluster)
--   - Smaller message size limit
--   - Less ecosystem support
--   - No built-in replication (use Redis replication)

-- Use Case:
--   - Use Redis Streams for:
--     * Low-latency event processing (< 1ms)
--     * Real-time notifications
--     * Cache invalidation events
--     * Short-lived events (1 hour TTL)
--   - Use Kafka for:
--     * Long-term event storage
--     * High-throughput event streaming (1M+ msg/sec)
--     * Event replay and time travel
--     * Multi-tenant event isolation

-- ============================================================================
-- EXAMPLE WORKFLOW
-- ============================================================================

-- 1. Producer adds event to stream
--    XADD events:stream * event-type payment.processed event-id evt-12345 data {...}

-- 2. Consumer reads from stream
--    XREADGROUP GROUP payment-service consumer-1 STREAMS events:stream >

-- 3. Consumer processes event
--    // Process event data
--    fmt.Println(eventType, eventID, data)

-- 4. Consumer acknowledges processing
--    XACK events:stream payment-service event-id

-- 5. If processing fails, message remains in pending list
--    XCLAIM events:stream payment-service consumer-2 60000 1526984818136-0

-- ============================================================================
-- CLUSTER CONSIDERATIONS
-- ============================================================================

-- In Redis Cluster mode:
--   - Stream is distributed across cluster slots
--   - Client must handle MOVED redirection
--   - Use hash slot for stream key
--   - Multiple streams for data distribution

-- Stream key distribution:
--   events:stream%         -> Slot 15595
--   events:stream#         -> Slot 3875
--   events:stream&         -> Slot 10152

-- Use hash tags for co-location:
--   events:{stream}:stream  -> Same slot for all keys
--   events:{stream}:consumer -> Same slot for all keys
