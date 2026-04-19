package com.yuan.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

public class MyDataSource {
    private static final LinkedList<Connection> pool = new LinkedList<>();

    static{
        try{
            Class.forName(DbConfig.driver);
            for (int i = 0; i < DbConfig.init_size; i++) {
                pool.add(createConnection());
            }
            AppLogger.getLogger().info("【DB连接池】初始化成功，已建立 " + DbConfig.init_size + " 个连接。");
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "【DB连接池】初始化彻底失败！", e);
        }
    }

    private static Connection createConnection () throws SQLException {
        return  DriverManager.getConnection(DbConfig.url,DbConfig.user,DbConfig.password);
    }

   public static Connection getConnection () throws SQLException {
        synchronized (pool){
            if (pool.isEmpty()) {
                AppLogger.getLogger().warning("【DB连接池】池子空了，正在创建临时连接！");
                return createConnection();
            } else {
                AppLogger.getLogger().fine("【DB连接池】借出一个连接，剩余: " + pool.size());
                return pool.removeFirst();
            }
        }
    }

    public static void releaseConnection(Connection conn){
        if (conn == null) {
            AppLogger.getLogger().warning("尝试空连接，已忽略");
            return;
        }
        synchronized (pool) {
            if (pool.size() < DbConfig.max_size) {
                AppLogger.getLogger().info("连接成功，当前可用数：" + pool.size());
                pool.addLast(conn);
            } else {
                try {
                    conn.close();
                    AppLogger.getLogger().info("【DB连接池】已销毁连接");
                } catch (SQLException e) {
                    AppLogger.getLogger().log(Level.SEVERE, "【DB连接池】关闭连接发生异常！", e);
                }
            }
        }
    }

}
