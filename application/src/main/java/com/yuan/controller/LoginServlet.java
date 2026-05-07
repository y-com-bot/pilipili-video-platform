package com.yuan.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.exception.AppException;
import com.yuan.service.UserService;
import com.yuan.utils.JsonUtils;
import com.yuan.utils.JwtUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/auth")
public class LoginServlet {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/login")
    @MyResponseBody
    public String login(@MyRequestParam("username") String username,
                        @MyRequestParam("password") String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AppException(400, "用户名和密码不能为空");
        }

        String result = userService.login(username.trim(), password);
        if (!result.startsWith("SUCCESS:")) {
            throw new AppException(400, result);
        }

        String token = result.substring("SUCCESS:".length());
        DecodedJWT jwt = JwtUtils.verifyToken(token);
        String role = jwt == null ? "user" : jwt.getClaim("role").asString();
        Long userId = jwt == null ? null : jwt.getClaim("userId").asLong();

        return "{\"code\":200,\"success\":true,\"message\":\"登录成功\","
                + "\"token\":\"" + JsonUtils.escape(token) + "\","
                + "\"role\":\"" + JsonUtils.escape(role) + "\","
                + "\"userId\":" + (userId == null ? "null" : userId) + "}";
    }
}
