package com.yuan.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

public class MyDataSource {
    private static final LinkedList<Connection> POOL = new LinkedList<>();
    private static final ThreadLocal<Connection> TX_CONNECTION_HOLDER = new ThreadLocal<>();

    static {
        try {
            Class.forName(DbConfig.driver);
            for (int i = 0; i < DbConfig.init_size; i++) {
                POOL.add(createConnection());
            }
            AppLogger.getLogger().info("Database connection pool initialized, size=" + DbConfig.init_size);
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Failed to initialize database connection pool", e);
        }
    }

    private static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
    }

    private static Connection acquirePoolConnection() throws SQLException {
        synchronized (POOL) {
            if (POOL.isEmpty()) {
                AppLogger.getLogger().warning("Connection pool exhausted, creating temporary connection");
                return createConnection();
            }
            return POOL.removeFirst();
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection transactionalConnection = TX_CONNECTION_HOLDER.get();
        if (transactionalConnection != null) {
            return transactionalConnection;
        }
        return acquirePoolConnection();
    }

    public static void beginTransaction() throws SQLException {
        if (TX_CONNECTION_HOLDER.get() != null) {
            throw new SQLException("Transaction already active on current thread");
        }

        Connection connection = acquirePoolConnection();
        connection.setAutoCommit(false);
        TX_CONNECTION_HOLDER.set(connection);
    }

    public static void commitTransaction() throws SQLException {
        Connection connection = TX_CONNECTION_HOLDER.get();
        if (connection != null) {
            connection.commit();
        }
    }

    public static void rollbackTransaction() {
        Connection connection = TX_CONNECTION_HOLDER.get();
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Failed to roll back transaction", e);
        }
    }

    public static void endTransaction() {
        Connection connection = TX_CONNECTION_HOLDER.get();
        TX_CONNECTION_HOLDER.remove();
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.WARNING, "Failed to reset connection auto-commit", e);
        } finally {
            releaseConnection(connection);
        }
    }

    public static boolean hasActiveTransaction() {
        return TX_CONNECTION_HOLDER.get() != null;
    }

    public static void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        Connection transactionalConnection = TX_CONNECTION_HOLDER.get();
        if (transactionalConnection != null && transactionalConnection == conn) {
            return;
        }

        synchronized (POOL) {
            try {
                if (conn.isClosed()) {
                    return;
                }
                if (POOL.size() < DbConfig.max_size) {
                    POOL.addLast(conn);
                } else {
                    conn.close();
                }
            } catch (SQLException e) {
                AppLogger.getLogger().log(Level.SEVERE, "Failed to release database connection", e);
            }
        }
    }
}
