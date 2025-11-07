package com.droid.bss.domain.datasource;

/**
 * Exception thrown when data source operations fail
 */
public class DataSourceException extends Exception {
    public DataSourceException(String message) {
        super(message);
    }

    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
