package com.yuan.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.exception.AppException;
import com.yuan.utils.AppLogger;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.TokenCache;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;

@WebFilter("/*")
public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String uri = req.getRequestURI();

        if (uri.contains("/login") || uri.contains("/register") || uri.endsWith(".html")
                || (uri.contains("/video") && req.getMethod().equalsIgnoreCase("GET"))
                || (uri.contains("/comment") && req.getMethod().equalsIgnoreCase("GET"))) {
            chain.doFilter(request, response);
            return;
        }

        String token = req.getHeader("Authorization");

        if (token == null || token.trim().isEmpty()) {
            AppLogger.getLogger().warning("未登录请求：" + uri + " 来自 " + req.getRemoteAddr());
            throw new AppException(401, "未登录，请登录获取token");
        }

        DecodedJWT jwt = JwtUtils.verifyToken(token);
        if (jwt == null) {
            AppLogger.getLogger().warning("无效或过期 token 请求：" + uri + " 来自 " + req.getRemoteAddr());
            throw new AppException(401, "token过期或无效");
        }

        Long userId = jwt.getClaim("userId").asLong();
        if (!TokenCache.isTokenValid(userId, token)) {
            AppLogger.getLogger().warning("Token 失效：userId=" + userId + " uri=" + uri);
            throw new AppException(401, "您的账号已在其他设备登录，当前 Token 已失效！");
        }

        String role = jwt.getClaim("role").asString();
        if (uri.startsWith("/admin")) {
            if (!"admin".equalsIgnoreCase(role)) {
                throw new AppException(403, "您没有权限访问该资源");
            }
        }

        req.setAttribute("currentRole", role);
        req.setAttribute("currentUserId", userId);
        chain.doFilter(req, resp);
    }
}
