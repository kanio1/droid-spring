package com.droid.bss.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for Redis connection
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test Redis connection by executing a simple PING command
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            if ("PONG".equals(result)) {
                return Health.up()
                        .withDetail("status", "Redis is available")
                        .withDetail("ping", result)
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "Redis is not responding correctly")
                        .withDetail("ping", result)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Redis connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
