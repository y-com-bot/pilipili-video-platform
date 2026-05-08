package com.yuan.utils;

import java.util.concurrent.ConcurrentHashMap;

public final class TokenCache {

    private static final ConcurrentHashMap<Long, String> ACTIVE_USERS = new ConcurrentHashMap<>();

    private TokenCache() {
    }

    public static void saveLoginStatus(Long userId, String token) {
        ACTIVE_USERS.put(userId, token);
        AppLogger.getLogger().info("Active user session saved: " + userId);
    }

    public static void clearLoginStatus(Long userId) {
        ACTIVE_USERS.remove(userId);
        AppLogger.getLogger().info("Active user session cleared: " + userId);
    }

    public static boolean isTokenValid(Long userId, String token) {
        String cachedToken = ACTIVE_USERS.get(userId);
        return cachedToken != null && cachedToken.equals(token);
    }
}
