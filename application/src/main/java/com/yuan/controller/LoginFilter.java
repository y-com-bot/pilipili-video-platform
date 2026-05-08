package com.yuan.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.exception.AppException;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.TokenCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import springMVC.MyHandlerInterceptor;

import java.util.Map;
import java.util.Set;

public class LoginFilter implements MyHandlerInterceptor {

    private static final String PUBLIC_PERMISSION = "public";
    private static final Map<String, String> URI_PERMISSIONS = Map.ofEntries(
            Map.entry("/api/auth/login", PUBLIC_PERMISSION),
            Map.entry("/api/auth/register", PUBLIC_PERMISSION),
            Map.entry("/api/auth/admin/register", PUBLIC_PERMISSION),
            Map.entry("/api/auth/refresh", "auth:refresh"),
            Map.entry("/api/videos/list", PUBLIC_PERMISSION),
            Map.entry("/api/videos/detail", PUBLIC_PERMISSION),
            Map.entry("/api/videos/publish", "video:publish"),
            Map.entry("/api/coupons/video", PUBLIC_PERMISSION),
            Map.entry("/api/coupons/claim", "coupon:claim"),
            Map.entry("/api/dynamics/detail", PUBLIC_PERMISSION),
            Map.entry("/api/dynamics/publish", "dynamic:publish"),
            Map.entry("/api/comments/list", PUBLIC_PERMISSION),
            Map.entry("/api/comments/create", "comment:create"),
            Map.entry("/api/comments/delete", "comment:delete"),
            Map.entry("/api/feed/list", PUBLIC_PERMISSION),
            Map.entry("/api/feed/categories", PUBLIC_PERMISSION),
            Map.entry("/api/social/follow/toggle", "social:follow"),
            Map.entry("/api/social/favorite/toggle", "social:favorite"),
            Map.entry("/api/social/follow/list", PUBLIC_PERMISSION),
            Map.entry("/api/social/followers/count", PUBLIC_PERMISSION),
            Map.entry("/api/likes/toggle", "like:toggle"),
            Map.entry("/api/admin/list", "admin:video:list"),
            Map.entry("/api/admin/stats", "admin:stats:read"),
            Map.entry("/api/admin/deleteVideo", "admin:video:delete"),
            Map.entry("/api/admin/deleteComment", "admin:comment:delete"),
            Map.entry("/api/admin/coupons/create", "admin:coupon:create"),
            Map.entry("/api/admin/coupons/listByVideo", "admin:coupon:list"),
            Map.entry("/api/admin/coupons/adjustStock", "admin:coupon:adjust"),
            Map.entry("/api/system/ping", PUBLIC_PERMISSION)
    );
    private static final Set<String> USER_PERMISSIONS = Set.of(
            "auth:refresh",
            "video:publish",
            "coupon:claim",
            "dynamic:publish",
            "comment:create",
            "comment:delete",
            "social:follow",
            "social:favorite",
            "like:toggle"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI().replace(request.getContextPath(), "");
        String token = request.getHeader("Authorization");
        String requiredPermission = URI_PERMISSIONS.getOrDefault(uri, PUBLIC_PERMISSION);

        if (PUBLIC_PERMISSION.equals(requiredPermission)) {
            bindCurrentUserIfPossible(request, token);
            return true;
        }

        if (token == null || token.trim().isEmpty()) {
            throw new AppException(401, "login required");
        }

        bindCurrentUserOrThrow(request, token.trim());
        String role = (String) request.getAttribute("currentRole");
        if (!hasPermission(role, requiredPermission)) {
            throw new AppException(403, "permission denied");
        }
        return true;
    }

    private void bindCurrentUserIfPossible(HttpServletRequest request, String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            bindCurrentUserOrThrow(request, token.trim());
        } catch (AppException ignored) {
        }
    }

    private void bindCurrentUserOrThrow(HttpServletRequest request, String token) {
        DecodedJWT jwt = JwtUtils.verifyToken(token);
        if (jwt == null) {
            throw new AppException(401, "token expired or invalid");
        }

        Long userId = jwt.getClaim("userId").asLong();
        String role = jwt.getClaim("role").asString();
        String username = jwt.getClaim("username").asString();
        if (userId == null || !TokenCache.isTokenValid(userId, token)) {
            throw new AppException(401, "login session invalid");
        }

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentRole", role);
        request.setAttribute("currentUsername", username);
    }

    private boolean hasPermission(String role, String permission) {
        if (PUBLIC_PERMISSION.equals(permission)) {
            return true;
        }
        if ("admin".equalsIgnoreCase(role)) {
            return true;
        }
        return "user".equalsIgnoreCase(role) && USER_PERMISSIONS.contains(permission);
    }
}
