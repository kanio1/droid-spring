package com.droid.bss.infrastructure.database.sharding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for shard manager.
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "bss.shard.manager")
public class ShardManagerProperties {

    private Boolean enabled = true;
    private String strategyType = "HASH";
    private Integer hashMultiplier = 31;
    private Long minValue;
    private Long maxValue;
    private Map<String, long[]> ranges;
    private List<Shard> shards;
    private Integer broadcastPoolSize;
    private Boolean cacheEnabled = true;
    private Long cacheTtlSeconds = 300L;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public Integer getHashMultiplier() {
        return hashMultiplier;
    }

    public void setHashMultiplier(Integer hashMultiplier) {
        this.hashMultiplier = hashMultiplier;
    }

    public Long getMinValue() {
        return minValue;
    }

    public void setMinValue(Long minValue) {
        this.minValue = minValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Long maxValue) {
        this.maxValue = maxValue;
    }

    public Map<String, long[]> getRanges() {
        return ranges;
    }

    public void setRanges(Map<String, long[]> ranges) {
        this.ranges = ranges;
    }

    public List<Shard> getShards() {
        return shards;
    }

    public void setShards(List<Shard> shards) {
        this.shards = shards;
    }

    public Integer getBroadcastPoolSize() {
        return broadcastPoolSize;
    }

    public void setBroadcastPoolSize(Integer broadcastPoolSize) {
        this.broadcastPoolSize = broadcastPoolSize;
    }

    public Boolean getCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(Boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public Long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(Long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }
}
