package com.droid.bss.infrastructure.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stub class for DomainEvent
 * Minimal implementation for testing purposes
 */
public class DomainEvent {

    private String id;
    private String type;
    private String source;
    private LocalDateTime time;
    private Object data;

    public DomainEvent() {
        this.id = UUID.randomUUID().toString();
        this.time = LocalDateTime.now();
    }

    public DomainEvent(String type, String source, Object data) {
        this();
        this.type = type;
        this.source = source;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Object getData() {
        return data;
    }
}
