package com.droid.bss.infrastructure.database.sharding;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a database shard.
 *
 * @since 1.0
 */
public class Shard {

    private final String id;
    private final String name;
    private final String connectionUrl;
    private final String databaseName;
    private final int weight;
    private final ShardStatus status;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final Integer maxPoolSize;
    private final Integer minPoolSize;
    private final Long connectionTimeout;

    private Shard(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.connectionUrl = builder.connectionUrl;
        this.databaseName = builder.databaseName;
        this.weight = builder.weight;
        this.status = builder.status != null ? builder.status : ShardStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.username = builder.username;
        this.password = builder.password;
        this.driverClassName = builder.driverClassName;
        this.maxPoolSize = builder.maxPoolSize;
        this.minPoolSize = builder.minPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
    }

    /**
     * Creates a new Builder for Shard.
     *
     * @return the builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new Shard with required fields.
     *
     * @param name the shard name
     * @param connectionUrl the connection URL
     * @return the shard
     */
    public static Shard create(String name, String connectionUrl) {
        return newBuilder()
            .name(name)
            .connectionUrl(connectionUrl)
            .build();
    }

    /**
     * Builder for Shard.
     */
    public static class Builder {
        private String id;
        private String name;
        private String connectionUrl;
        private String databaseName;
        private int weight = 1;
        private ShardStatus status = ShardStatus.ACTIVE;
        private String username;
        private String password;
        private String driverClassName;
        private Integer maxPoolSize;
        private Integer minPoolSize;
        private Long connectionTimeout;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder connectionUrl(String connectionUrl) {
            this.connectionUrl = connectionUrl;
            return this;
        }

        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder weight(int weight) {
            if (weight < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }
            this.weight = weight;
            return this;
        }

        public Builder status(ShardStatus status) {
            this.status = status;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder maxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder minPoolSize(Integer minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder connectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Shard build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Shard name cannot be null or blank");
            }
            if (connectionUrl == null || connectionUrl.isBlank()) {
                throw new IllegalArgumentException("Connection URL cannot be null or blank");
            }
            return new Shard(this);
        }
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public int getWeight() {
        return weight;
    }

    public ShardStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Updates the shard status.
     *
     * @param newStatus the new status
     * @return this shard for chaining
     */
    public Shard updateStatus(ShardStatus newStatus) {
        if (this.status == newStatus) {
            return this;
        }
        return newBuilder()
            .id(this.id)
            .name(this.name)
            .connectionUrl(this.connectionUrl)
            .databaseName(this.databaseName)
            .weight(this.weight)
            .status(newStatus)
            .username(this.username)
            .password(this.password)
            .driverClassName(this.driverClassName)
            .maxPoolSize(this.maxPoolSize)
            .minPoolSize(this.minPoolSize)
            .connectionTimeout(this.connectionTimeout)
            .build();
    }

    /**
     * Checks if the shard is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return status == ShardStatus.ACTIVE;
    }

    /**
     * Checks if the shard is unavailable.
     *
     * @return true if unavailable
     */
    public boolean isUnavailable() {
        return status == ShardStatus.UNAVAILABLE;
    }

    /**
     * Checks if the shard is draining.
     *
     * @return true if draining
     */
    public boolean isDraining() {
        return status == ShardStatus.DRAINING;
    }

    /**
     * Gets the age of the shard in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getAgeMs() {
        return java.time.Duration.between(createdAt, java.time.Instant.now()).toMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shard shard = (Shard) o;
        return Objects.equals(id, shard.id) || Objects.equals(connectionUrl, shard.connectionUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, connectionUrl);
    }

    @Override
    public String toString() {
        return "Shard{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", status=" + status +
            ", weight=" + weight +
            ", ageMs=" + getAgeMs() +
            '}';
    }
}
