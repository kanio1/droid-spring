package com.droid.bss.infrastructure.rsocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-based connection registry for RSocket
 * Enables multi-instance scalability by storing connection metadata in Redis
 * while each instance maintains its local RSocketRequester connections
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConnectionRegistry {

    private static final String CONNECTION_PREFIX = "rsocket:connection:";
    private static final String USER_CONNECTIONS_PREFIX = "rsocket:user:";
    private static final String CONNECTION_COUNT_KEY = "rsocket:stats:totalConnections";
    private static final long CONNECTION_TTL_SECONDS = 3600; // 1 hour

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, RSocketRequester> localConnections = new ConcurrentHashMap<>();
    private final Map<String, String> userSubscriptions = new ConcurrentHashMap<>();

    /**
     * Register a new RSocket connection
     */
    public void registerConnection(String clientId, RSocketRequester requester, String userId, String clientInfo) {
        String connectionKey = CONNECTION_PREFIX + clientId;
        String userConnectionsKey = USER_CONNECTIONS_PREFIX + userId;

        // Store connection metadata in Redis
        ConnectionMetadata metadata = ConnectionMetadata.builder()
            .clientId(clientId)
            .userId(userId)
            .clientInfo(clientInfo)
            .instanceId(getInstanceId())
            .connectedAt(LocalDateTime.now())
            .lastSeen(LocalDateTime.now())
            .build();

        redisTemplate.opsForValue().set(connectionKey, metadata, java.time.Duration.ofSeconds(CONNECTION_TTL_SECONDS));
        redisTemplate.opsForValue().set(userConnectionsKey + ":" + clientId, clientId, java.time.Duration.ofSeconds(CONNECTION_TTL_SECONDS));

        // Store local reference for sending messages
        localConnections.put(clientId, requester);

        // Update connection count
        redisTemplate.opsForValue().increment(CONNECTION_COUNT_KEY);

        log.info("Registered RSocket connection: clientId={}, userId={}, instanceId={}, totalConnections={}",
            clientId, userId, getInstanceId(), getTotalConnections());
    }

    /**
     * Unregister a connection
     */
    public void unregisterConnection(String clientId) {
        ConnectionMetadata metadata = getConnectionMetadata(clientId);
        if (metadata != null) {
            String userConnectionsKey = USER_CONNECTIONS_PREFIX + metadata.getUserId();

            // Remove from Redis
            redisTemplate.delete(CONNECTION_PREFIX + clientId);
            redisTemplate.delete(userConnectionsKey + ":" + clientId);
            redisTemplate.opsForValue().decrement(CONNECTION_COUNT_KEY);

            // Remove local reference
            localConnections.remove(clientId);
            userSubscriptions.remove(clientId);

            log.info("Unregistered RSocket connection: clientId={}, userId={}, totalConnections={}",
                clientId, metadata.getUserId(), getTotalConnections());
        }
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen(String clientId) {
        ConnectionMetadata metadata = getConnectionMetadata(clientId);
        if (metadata != null) {
            metadata.setLastSeen(LocalDateTime.now());
            redisTemplate.opsForValue().set(CONNECTION_PREFIX + clientId, metadata, java.time.Duration.ofSeconds(CONNECTION_TTL_SECONDS));
        }
    }

    /**
     * Get connection metadata
     */
    public ConnectionMetadata getConnectionMetadata(String clientId) {
        return (ConnectionMetadata) redisTemplate.opsForValue().get(CONNECTION_PREFIX + clientId);
    }

    /**
     * Get all client IDs for a user
     */
    public java.util.List<String> getUserConnections(String userId) {
        String userConnectionsPrefix = USER_CONNECTIONS_PREFIX + userId + ":";
        return redisTemplate.keys(userConnectionsPrefix + "*").stream()
            .map(key -> key.substring(userConnectionsPrefix.length()))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get local connection
     */
    public RSocketRequester getLocalConnection(String clientId) {
        return localConnections.get(clientId);
    }

    /**
     * Get all local connections
     */
    public Map<String, RSocketRequester> getAllLocalConnections() {
        return new ConcurrentHashMap<>(localConnections);
    }

    /**
     * Add subscription for a connection
     */
    public void addSubscription(String clientId, String subscriptionType) {
        userSubscriptions.merge(clientId, subscriptionType, (old, newVal) -> old + "," + newVal);
    }

    /**
     * Get subscription for a connection
     */
    public String getSubscriptions(String clientId) {
        return userSubscriptions.get(clientId);
    }

    /**
     * Clean up stale connections
     */
    public void cleanupStaleConnections() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        String pattern = CONNECTION_PREFIX + "*";

        redisTemplate.keys(pattern).forEach(key -> {
            ConnectionMetadata metadata = (ConnectionMetadata) redisTemplate.opsForValue().get(key);
            if (metadata != null && metadata.getLastSeen().isBefore(cutoff)) {
                String clientId = metadata.getClientId();
                log.warn("Removing stale connection: {}", clientId);
                unregisterConnection(clientId);
            }
        });
    }

    /**
     * Get total connections across all instances
     */
    public long getTotalConnections() {
        Object value = redisTemplate.opsForValue().get(CONNECTION_COUNT_KEY);
        return value instanceof Long ? (Long) value : 0L;
    }

    /**
     * Get local connections count
     */
    public int getLocalConnectionsCount() {
        return localConnections.size();
    }

    /**
     * Check if connection exists
     */
    public boolean connectionExists(String clientId) {
        return redisTemplate.hasKey(CONNECTION_PREFIX + clientId);
    }

    private String getInstanceId() {
        return System.getProperty("instance.id", "default-instance");
    }

    /**
     * Connection metadata stored in Redis
     */
    public static class ConnectionMetadata {
        private String clientId;
        private String userId;
        private String clientInfo;
        private String instanceId;
        private LocalDateTime connectedAt;
        private LocalDateTime lastSeen;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getClientInfo() { return clientInfo; }
        public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public LocalDateTime getConnectedAt() { return connectedAt; }
        public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }

        public LocalDateTime getLastSeen() { return lastSeen; }
        public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String clientId;
            private String userId;
            private String clientInfo;
            private String instanceId;
            private LocalDateTime connectedAt;
            private LocalDateTime lastSeen;

            public Builder clientId(String clientId) {
                this.clientId = clientId;
                return this;
            }

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder clientInfo(String clientInfo) {
                this.clientInfo = clientInfo;
                return this;
            }

            public Builder instanceId(String instanceId) {
                this.instanceId = instanceId;
                return this;
            }

            public Builder connectedAt(LocalDateTime connectedAt) {
                this.connectedAt = connectedAt;
                return this;
            }

            public Builder lastSeen(LocalDateTime lastSeen) {
                this.lastSeen = lastSeen;
                return this;
            }

            public ConnectionMetadata build() {
                ConnectionMetadata metadata = new ConnectionMetadata();
                metadata.setClientId(clientId);
                metadata.setUserId(userId);
                metadata.setClientInfo(clientInfo);
                metadata.setInstanceId(instanceId);
                metadata.setConnectedAt(connectedAt);
                metadata.setLastSeen(lastSeen);
                return metadata;
            }
        }
    }
}
