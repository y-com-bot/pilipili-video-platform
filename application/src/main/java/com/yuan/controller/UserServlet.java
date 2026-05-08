package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.service.UserService;
import com.yuan.utils.JsonUtils;
import springMVC.MyAutowired;
import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyRequestParam;
import springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/auth")
public class UserServlet extends BaseApiController {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/register")
    @MyResponseBody
    public String register(@MyRequestParam("username") String username,
                           @MyRequestParam("password") String password) {
        String safeUsername = trimToNull(username);
        String safePassword = trimToNull(password);
        if (safeUsername == null || safePassword == null) {
            throw new AppException(400, "username and password are required");
        }

        String message = userService.register(safeUsername, safePassword);
        return JsonUtils.success(message);
    }

    @MyRequestMapping("/admin/register")
    @MyResponseBody
    public String registerAdmin(@MyRequestParam("username") String username,
                                @MyRequestParam("password") String password) {
        String safeUsername = trimToNull(username);
        String safePassword = trimToNull(password);
        if (safeUsername == null || safePassword == null) {
            throw new AppException(400, "username and password are required");
        }

        String message = userService.registerAdmin(safeUsername, safePassword);
        return JsonUtils.success(message);
    }
}
