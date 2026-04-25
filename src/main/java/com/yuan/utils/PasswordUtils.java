package com.yuan.utils;

import com.yuan.utils.AppLogger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;

public class PasswordUtils {
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
    public static String encryptPassword(String password, String salt){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = password + salt;
            byte[] hashedBytes = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            AppLogger.getLogger().log(Level.SEVERE, "获取算法发生异常", e);
            throw new RuntimeException(e);
        }
    }

    public static boolean checkPassword(String inputPassword, String salt, String savedHash){
        String NewHash = encryptPassword(inputPassword,salt);
        return NewHash.equals(savedHash);
    }
}
