package com.yuan.utils;

import com.yuan.utils.AppLogger;
import java.util.concurrent.ConcurrentHashMap;

public class TokenCache {

    private static final ConcurrentHashMap<Long, String> activeUsers = new ConcurrentHashMap<>();

    public static void saveLoginStatus(Long userId, String token) {
        activeUsers.put(userId, token);
        AppLogger.getLogger().info("Active user saved: " + userId + " -> " + token);
    }

    public static void clearLoginStatus(Long userId) {
        activeUsers.remove(userId);
        AppLogger.getLogger().info("用户ID: " + userId + " 已退出登录。");
    }

    public static boolean isTokenValid(Long userId, String token) {
        if(activeUsers.containsKey(userId) && activeUsers.get(userId).equals(token))
            return true;
        else
            return false;
    }
}
