package com.droid.bss.infrastructure.database.sharding;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Configuration for shard management infrastructure.
 *
 * @since 1.0
 */
@Configuration
@ConditionalOnClass({ShardManager.class, ShardingStrategy.class})
@EnableConfigurationProperties(ShardManagerProperties.class)
public class ShardManagerConfig {

    /**
     * Creates the default shard manager bean.
     *
     * @param properties the shard manager properties
     * @return the shard manager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "bss.shard.manager.enabled", havingValue = "true", matchIfMissing = true)
    public ShardManager shardManager(ShardManagerProperties properties) {
        ShardingStrategy strategy = createShardingStrategy(properties);
        DefaultShardManager manager = new DefaultShardManager(strategy);

        if (properties.getShards() != null && !properties.getShards().isEmpty()) {
            for (Shard shard : properties.getShards()) {
                manager.registerShard(shard);
            }
        }

        return manager;
    }

    /**
     * Creates the broadcast executor bean.
     *
     * @param properties the shard manager properties
     * @return the executor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "bss.shard.manager.enabled", havingValue = "true", matchIfMissing = true)
    public Executor shardBroadcastExecutor(ShardManagerProperties properties) {
        int poolSize = properties.getBroadcastPoolSize() != null
            ? properties.getBroadcastPoolSize()
            : Runtime.getRuntime().availableProcessors() * 2;

        return new ForkJoinPool(poolSize,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true);
    }

    /**
     * Creates a data source for a shard.
     *
     * @param shard the shard
     * @return the data source
     */
    @Bean
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnProperty(name = "bss.shard.manager.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource shardDataSource(Shard shard) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(shard.getConnectionUrl());
        config.setUsername(shard.getUsername());
        config.setPassword(shard.getPassword());
        config.setDriverClassName(shard.getDriverClassName());

        config.setMaximumPoolSize(shard.getMaxPoolSize() != null ? shard.getMaxPoolSize() : 10);
        config.setMinimumIdle(shard.getMinPoolSize() != null ? shard.getMinPoolSize() : 5);
        config.setConnectionTimeout(shard.getConnectionTimeout() != null ? shard.getConnectionTimeout() : 30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    /**
     * Creates the sharding strategy based on configuration.
     *
     * @param properties the properties
     * @return the sharding strategy
     */
    private ShardingStrategy createShardingStrategy(ShardManagerProperties properties) {
        String strategyType = properties.getStrategyType() != null
            ? properties.getStrategyType().toUpperCase()
            : "HASH";

        return switch (strategyType) {
            case "HASH" -> {
                int multiplier = properties.getHashMultiplier() != null
                    ? properties.getHashMultiplier()
                    : 31;
                yield new HashShardingStrategy(multiplier);
            }
            case "RANGE" -> {
                long minValue = properties.getMinValue() != null
                    ? properties.getMinValue()
                    : 0L;
                long maxValue = properties.getMaxValue() != null
                    ? properties.getMaxValue()
                    : Long.MAX_VALUE;

                RangeShardingStrategy rangeStrategy = new RangeShardingStrategy(minValue, maxValue);

                if (properties.getRanges() != null) {
                    for (Map.Entry<String, long[]> entry : properties.getRanges().entrySet()) {
                        String shardId = entry.getKey();
                        long[] range = entry.getValue();
                        rangeStrategy.addRange(range[0], range[1], shardId);
                    }
                }

                yield rangeStrategy;
            }
            default -> throw new IllegalArgumentException("Unknown sharding strategy: " + strategyType);
        };
    }

    /**
     * Registers a {@link Shard} bean for each configured shard.
     *
     * @param shardId the shard ID
     * @param properties the shard manager properties
     * @return the shard bean name
     */
    @Bean
    @ConditionalOnProperty(name = "bss.shard.manager.enabled", havingValue = "true", matchIfMissing = true)
    public String shardBeanName(ShardManagerProperties properties) {
        return "shardBean";
    }
}
