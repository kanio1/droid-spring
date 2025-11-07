package com.droid.bss.infrastructure.database.pooling;

import javax.sql.DataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Stub class for Hikari connection pool
 * Minimal implementation for testing purposes
 */
public class HikariConnectionPool implements Closeable {

    private final ConnectionPoolConfig config;
    private boolean closed = false;

    public HikariConnectionPool(ConnectionPoolConfig config) {
        this.config = config;
    }

    public ConnectionPoolConfig getConfig() {
        return config;
    }

    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }
        // Stub implementation - return null connection
        return null;
    }

    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
