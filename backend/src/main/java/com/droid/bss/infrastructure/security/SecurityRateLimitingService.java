package com.droid.bss.infrastructure.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting using Redis
 */
@Service
public class SecurityRateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    public SecurityRateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if the request is allowed based on rate limiting rules
     *
     * @param key       Redis key for tracking
     * @param maxRequests Maximum number of requests allowed
     * @param timeWindow Time window in seconds
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, int timeWindow) {
        String luaScript =
                "local current = redis.call('GET', KEYS[1]) " +
                "if current == false then " +
                "  redis.call('SET', KEYS[1], 1) " +
                "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                "  return 1 " +
                "else " +
                "  local count = tonumber(current) " +
                "  if count < tonumber(ARGV[1]) then " +
                "    redis.call('INCR', KEYS[1]) " +
                "    return count + 1 " +
                "  else " +
                "    return 0 " +
                "  end " +
                "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

        String redisKey = key;
        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey),
                String.valueOf(maxRequests), String.valueOf(timeWindow));

        return result != null && result > 0;
    }

    /**
     * Get the rate limiting key for the current request
     */
    public String getRateLimitKey(String prefix) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String clientId = "anonymous";
        if (authentication != null && authentication.isAuthenticated()) {
            // Use user ID if available, otherwise use client_id from JWT
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                org.springframework.security.oauth2.jwt.Jwt jwt =
                    (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
                clientId = jwt.getClaimAsString("sub");
                if (clientId == null) {
                    clientId = jwt.getClaimAsString("client_id");
                }
            } else {
                clientId = authentication.getName();
            }
        }

        return prefix + ":" + clientId;
    }
}
