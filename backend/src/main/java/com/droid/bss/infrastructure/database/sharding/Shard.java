package com.droid.bss.infrastructure.database.sharding;

import java.util.UUID;

/**
 * Stub class for database sharding
 * Minimal implementation for testing purposes
 */
public class Shard {

    private final UUID id;
    private final String name;
    private final String connectionUrl;
    private final boolean active;

    public Shard(String name, String connectionUrl) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.connectionUrl = connectionUrl;
        this.active = true;
    }

    public Shard(UUID id, String name, String connectionUrl, boolean active) {
        this.id = id;
        this.name = name;
        this.connectionUrl = connectionUrl;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Shard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", connectionUrl='" + connectionUrl + '\'' +
                ", active=" + active +
                '}';
    }
}
