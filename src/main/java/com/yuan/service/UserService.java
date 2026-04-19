package com.yuan.service;

import com.yuan.dao.UserDAO;
import com.yuan.entity.User;
import com.yuan.exception.AppException;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.PasswordUtils;
import com.yuan.utils.TokenCache;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    public String register(String username, String password) {
        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null)
            return "注册失败，该用户名已存在";

        String salt = PasswordUtils.generateSalt();
        String encryptedPassword = PasswordUtils.encryptPassword(password, salt);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(encryptedPassword);
        newUser.setSalt(salt);

        String role = "user";
        if ("admin".equalsIgnoreCase(username)) {
            role = "admin";
        }
        newUser.setRole(role);

        boolean ifSuccess = userDAO.insertUser(newUser);

        if (!ifSuccess) {
            return "注册失败：系统内部错误";
        }
        return role.equals("admin") ? "注册成功，已创建管理员账号" : "注册成功";
    }

    public String login(String username, String rawPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new AppException(404, "账号不存在");
        }

        boolean ifCorrect = PasswordUtils.checkPassword(rawPassword, user.getSalt(), user.getPassword());
        if (!ifCorrect) {
            return "登录失败，密码错误";
        }

        String token = JwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());

        TokenCache.saveLoginStatus(user.getId(), token);

        return "SUCCESS:" + token;
    }
}