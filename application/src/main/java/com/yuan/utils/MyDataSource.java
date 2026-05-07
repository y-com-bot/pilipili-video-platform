package com.yuan.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * 一个很轻量的连接池实现。
 *
 * 对面试项目来说它已经够用：
 * 1. 启动时预热少量连接
 * 2. 借出连接后归还到池中
 * 3. 池满时关闭多余连接
 */
public class MyDataSource {
    private static final LinkedList<Connection> pool = new LinkedList<>();

    static {
        try {
            Class.forName(DbConfig.driver);
            for (int i = 0; i < DbConfig.init_size; i++) {
                pool.add(createConnection());
            }
            AppLogger.getLogger().info("数据库连接池初始化成功，连接数: " + DbConfig.init_size);
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "数据库连接池初始化失败", e);
        }
    }

    private static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
    }

    public static Connection getConnection() throws SQLException {
        synchronized (pool) {
            if (pool.isEmpty()) {
                AppLogger.getLogger().warning("连接池已空，创建临时连接");
                return createConnection();
            }
            return pool.removeFirst();
        }
    }

    public static void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        synchronized (pool) {
            try {
                if (conn.isClosed()) {
                    return;
                }
                if (pool.size() < DbConfig.max_size) {
                    pool.addLast(conn);
                } else {
                    conn.close();
                }
            } catch (SQLException e) {
                AppLogger.getLogger().log(Level.SEVERE, "归还数据库连接失败", e);
            }
        }
    }
}
