package com.yuan.utils;

public class DbConfig {
    public static final String url = "jdbc:mysql://localhost:3306/platform?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    public static final String user = "root";
    public static final String password = "1212";
    public static final String driver = "com.mysql.cj.jdbc.Driver";
    public static final int init_size = 5;
    public static final int max_size = 10;
}