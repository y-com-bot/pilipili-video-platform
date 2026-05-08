package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import springMVC.MyAutowired;
import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyRequestParam;
import springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/auth")
public class LoginServlet extends BaseApiController {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/login")
    @MyResponseBody
    public String login(@MyRequestParam("username") String username,
                        @MyRequestParam("password") String password) {
        String safeUsername = trimToNull(username);
        String safePassword = trimToNull(password);
        if (safeUsername == null || safePassword == null) {
            throw new AppException(400, "username and password are required");
        }

        UserService.AuthSession session = userService.login(safeUsername, safePassword);
        return toAuthJson("login success", session);
    }

    @MyRequestMapping("/refresh")
    @MyResponseBody
    public String refresh(HttpServletRequest request) {
        String currentToken = trimToNull(request.getHeader("Authorization"));
        UserService.AuthSession session = userService.refreshToken(currentToken);
        return toAuthJson("token refreshed", session);
    }

    private String toAuthJson(String message, UserService.AuthSession session) {
        return "{"
                + "\"code\":200,"
                + "\"success\":true,"
                + "\"message\":" + jsonString(message) + ","
                + "\"token\":" + jsonString(session.getToken()) + ","
                + "\"role\":" + jsonString(session.getRole()) + ","
                + "\"userId\":" + session.getUserId()
                + "}";
    }
}
