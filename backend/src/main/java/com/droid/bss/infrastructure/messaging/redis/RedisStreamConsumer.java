package com.droid.bss.infrastructure.messaging.redis;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumer for Redis Streams
 * Handles real-time event processing
 */
@Component
public class RedisStreamConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisStreamConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create consumer group
     */
    public void createConsumerGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, groupName);
        } catch (Exception e) {
            // Group might already exist
        }
    }

    /**
     * Read from stream
     */
    public Map<Object, Object> readFromStream(String streamKey) {
        return redisTemplate.opsForStream().read(streamKey);
    }

    /**
     * Read from stream with timeout
     */
    public Map<Object, Object> readFromStream(String streamKey, long timeoutSeconds) {
        return redisTemplate.opsForStream().read(streamKey, timeoutSeconds);
    }

    /**
     * Read from consumer group
     */
    public Map<Object, Object> readFromConsumerGroup(String streamKey, String groupName, String consumerName) {
        return redisTemplate.opsForStream().read(
            streamKey,
            org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed(),
            groupName,
            consumerName
        );
    }

    /**
     * Acknowledge message
     */
    public void acknowledge(String streamKey, String groupName, String messageId) {
        redisTemplate.opsForStream().acknowledge(streamKey, groupName, messageId);
    }

    /**
     * Delete message from stream
     */
    public void deleteMessage(String streamKey, String messageId) {
        redisTemplate.opsForStream().delete(streamKey, messageId);
    }

    /**
     * Get stream info
     */
    public Map<String, Object> getStreamInfo(String streamKey) {
        return redisTemplate.opsForStream().info(streamKey);
    }
}
