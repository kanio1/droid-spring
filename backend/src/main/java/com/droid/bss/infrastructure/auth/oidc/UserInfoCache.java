package com.droid.bss.infrastructure.auth.oidc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub class for user info cache
 * Minimal implementation for testing purposes
 */
public class UserInfoCache {

    private final ConcurrentHashMap<String, UserInfo> cache;

    public UserInfoCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String token, UserInfo userInfo) {
        cache.put(token, userInfo);
    }

    public UserInfo get(String token) {
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
 * Stub class for user info
 * Minimal implementation for testing purposes
 */
class UserInfo {

    private String sub;
    private String email;
    private String name;

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
