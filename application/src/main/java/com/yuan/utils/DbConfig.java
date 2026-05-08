package com.yuan.utils;

public class DbConfig {
    public static final String url = ConfigManager.getProperty(
            "db.url",
            "jdbc:mysql://localhost:3306/platform?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    );
    public static final String user = ConfigManager.getProperty("db.user", "root");
    public static final String password = ConfigManager.getProperty("db.password", "1212");
    public static final String driver = ConfigManager.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    public static final int init_size = Integer.parseInt(ConfigManager.getProperty("db.pool.init-size", "5"));
    public static final int max_size = Integer.parseInt(ConfigManager.getProperty("db.pool.max-size", "10"));
}
