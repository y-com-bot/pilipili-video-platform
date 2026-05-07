package com.yuan.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.exception.AppException;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.TokenCache;
import framework.springMVC.MyHandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 这是 application 模块接入 framework.springMVC 之后的鉴权拦截器。
 *
 * 它不再依赖容器级 Filter，而是走框架自己的拦截器链，
 * 这样更能体现“application 真正在使用 framework”。
 */
public class LoginFilter implements MyHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI().replace(request.getContextPath(), "");

        if (isPublicApi(uri)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            throw new AppException(401, "请先登录后再访问该接口");
        }

        DecodedJWT jwt = JwtUtils.verifyToken(token);
        if (jwt == null) {
            throw new AppException(401, "登录状态已失效，请重新登录");
        }

        Long userId = jwt.getClaim("userId").asLong();
        String role = jwt.getClaim("role").asString();
        if (userId == null || !TokenCache.isTokenValid(userId, token)) {
            throw new AppException(401, "当前账号已在其他设备登录，请重新登录");
        }

        if (uri.startsWith("/api/admin") && !"admin".equalsIgnoreCase(role)) {
            throw new AppException(403, "您没有权限访问管理员接口");
        }

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentRole", role);
        return true;
    }

    private boolean isPublicApi(String uri) {
        return uri.startsWith("/api/auth/")
                || "/api/videos/list".equals(uri)
                || "/api/videos/detail".equals(uri)
                || "/api/comments/list".equals(uri)
                || "/api/system/ping".equals(uri);
    }
}
