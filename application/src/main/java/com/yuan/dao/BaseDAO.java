package com.yuan.dao;

import com.yuan.utils.AppLogger;
import com.yuan.utils.MyDataSource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BaseDAO {
    public int update(String sql, Object... args) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Database update failed", e);
        } finally {
            closeResource(null, ps, null);
            MyDataSource.releaseConnection(conn);
        }
        return 0;
    }

    public Long insertAndReturnId(String sql, Object... args) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyDataSource.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            int rows = ps.executeUpdate();
            if (rows <= 0) {
                return null;
            }
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Insert and return id failed", e);
        } finally {
            closeResource(null, ps, rs);
            MyDataSource.releaseConnection(conn);
        }
        return null;
    }

    public <T> List<T> queryForList(Class<T> clazz, String sql, Object... args) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<T> list = new ArrayList<>();
        try {
            conn = MyDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();
                for (int i = 0; i < columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i + 1);
                    Object value = rs.getObject(columnName);

                    Field field = clazz.getDeclaredField(columnName);
                    field.setAccessible(true);
                    if (value != null
                            && value.getClass() == java.time.LocalDateTime.class
                            && field.getType() == java.util.Date.class) {
                        value = java.sql.Timestamp.valueOf((java.time.LocalDateTime) value);
                    }

                    field.set(entity, value);
                }
                list.add(entity);
            }
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Query list failed", e);
            throw new RuntimeException("Query list failed", e);
        } finally {
            closeResource(null, ps, rs);
            MyDataSource.releaseConnection(conn);
        }
        return list;
    }

    public Object queryForValue(String sql, Object... args) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getObject(1);
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Query single value failed", e);
        } finally {
            closeResource(null, ps, rs);
            MyDataSource.releaseConnection(conn);
        }
        return null;
    }

    protected Connection getConnection() throws SQLException {
        return MyDataSource.getConnection();
    }

    protected static void closeResource(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Close database resource failed", e);
        }
    }
}
