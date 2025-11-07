package com.droid.bss.infrastructure.auth.oidc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub class for token introspection cache
 * Minimal implementation for testing purposes
 */
public class TokenIntrospectionCache {

    private final ConcurrentHashMap<String, TokenIntrospection> cache;

    public TokenIntrospectionCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String token, TokenIntrospection introspection) {
        cache.put(token, introspection);
    }

    public TokenIntrospection get(String token) {
        return cache.get(token);
    }

    public void remove(String token) {
        cache.remove(token);
    }

    public void clear() {
        cache.clear();
    }
}

/**
 * Stub class for token introspection
 * Minimal implementation for testing purposes
 */
class TokenIntrospection {

    private boolean active;
    private String sub;
    private String scope;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
