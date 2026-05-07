package com.yuan.service;

import com.yuan.dao.UserDAO;
import com.yuan.entity.User;
import com.yuan.exception.AppException;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.PasswordUtils;
import com.yuan.utils.TokenCache;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyService;

@MyService
public class UserService {

    @MyAutowired
    private UserDAO userDAO;

    public String register(String username, String password) {
        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            throw new AppException(400, "该用户名已存在");
        }

        String salt = PasswordUtils.generateSalt();
        String encryptedPassword = PasswordUtils.encryptPassword(password, salt);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(encryptedPassword);
        newUser.setSalt(salt);
        newUser.setRole("admin".equalsIgnoreCase(username) ? "admin" : "user");

        if (!userDAO.insertUser(newUser)) {
            throw new AppException(500, "注册失败，请稍后再试");
        }
        return "注册成功";
    }

    public String login(String username, String rawPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new AppException(404, "账号不存在");
        }

        boolean correct = PasswordUtils.checkPassword(rawPassword, user.getSalt(), user.getPassword());
        if (!correct) {
            throw new AppException(400, "密码错误");
        }

        String token = JwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
        TokenCache.saveLoginStatus(user.getId(), token);
        return "SUCCESS:" + token;
    }
}
