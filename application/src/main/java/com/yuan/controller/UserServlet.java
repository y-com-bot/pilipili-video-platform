package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.service.UserService;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/auth")
public class UserServlet {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/register")
    @MyResponseBody
    public String register(@MyRequestParam("username") String username,
                           @MyRequestParam("password") String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AppException(400, "用户名和密码不能为空");
        }

        String message = userService.register(username.trim(), password);
        return JsonUtils.success(message);
    }
}
