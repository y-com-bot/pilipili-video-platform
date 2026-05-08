package com.yuan.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.dao.UserDAO;
import com.yuan.entity.User;
import com.yuan.exception.AppException;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.PasswordUtils;
import com.yuan.utils.TokenCache;
import springMVC.MyAutowired;
import springMVC.MyService;

@MyService
public class UserService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ADMIN = "admin";

    @MyAutowired
    private UserDAO userDAO;

    public String register(String username, String password) {
        registerInternal(username, password, ROLE_USER);
        return "register success";
    }

    public String registerAdmin(String username, String password) {
        if (userDAO.countByRole(ROLE_ADMIN) > 0) {
            throw new AppException(403, "admin registration is closed");
        }
        registerInternal(username, password, ROLE_ADMIN);
        return "admin register success";
    }

    public AuthSession login(String username, String rawPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new AppException(404, "account not found");
        }

        boolean correct = PasswordUtils.checkPassword(rawPassword, user.getSalt(), user.getPassword());
        if (!correct) {
            throw new AppException(400, "password incorrect");
        }

        return issueSession(user.getId(), user.getUsername(), user.getRole());
    }

    public AuthSession refreshToken(String currentToken) {
        if (currentToken == null || currentToken.trim().isEmpty()) {
            throw new AppException(401, "authorization token required");
        }

        DecodedJWT jwt = JwtUtils.verifyToken(currentToken.trim());
        if (jwt == null) {
            throw new AppException(401, "token expired or invalid");
        }

        Long userId = jwt.getClaim("userId").asLong();
        String username = jwt.getClaim("username").asString();
        String role = jwt.getClaim("role").asString();
        if (userId == null || username == null || role == null) {
            throw new AppException(401, "token payload invalid");
        }
        if (!TokenCache.isTokenValid(userId, currentToken.trim())) {
            throw new AppException(401, "login session invalid");
        }

        return issueSession(userId, username, role);
    }

    private void registerInternal(String username, String password, String role) {
        validateCredentials(username, password);

        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            throw new AppException(400, "username already exists");
        }

        String salt = PasswordUtils.generateSalt();
        String encryptedPassword = PasswordUtils.encryptPassword(password, salt);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(encryptedPassword);
        newUser.setSalt(salt);
        newUser.setRole(role);

        if (!userDAO.insertUser(newUser)) {
            throw new AppException(500, "register failed");
        }
    }

    private AuthSession issueSession(Long userId, String username, String role) {
        String token = JwtUtils.createToken(userId, username, role);
        TokenCache.saveLoginStatus(userId, token);
        return new AuthSession(token, userId, role);
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AppException(400, "username and password are required");
        }
        if (username.length() > 32) {
            throw new AppException(400, "username too long");
        }
        if (password.length() < 6) {
            throw new AppException(400, "password must be at least 6 characters");
        }
    }

    public static class AuthSession {
        private final String token;
        private final Long userId;
        private final String role;

        public AuthSession(String token, Long userId, String role) {
            this.token = token;
            this.userId = userId;
            this.role = role;
        }

        public String getToken() {
            return token;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }
}
