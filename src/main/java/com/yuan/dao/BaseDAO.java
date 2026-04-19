package com.yuan.dao;

import com.yuan.utils.AppLogger;
import com.yuan.utils.MyDataSource;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BaseDAO {
    public int update (String sql, Object ...args) {
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
            AppLogger.getLogger().log(Level.SEVERE, "获取连接发生异常", e);
        } finally {
            MyDataSource.releaseConnection(conn);
            BaseDAO.closeResource(null, ps, null);
        }
        return 0;
    }

    public <T> List<T> queryForList(Class<T> clazz, String sql,Object...args) {
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
                    if (value != null && value.getClass() == java.time.LocalDateTime.class && field.getType() == java.util.Date.class) {
                        value = java.sql.Timestamp.valueOf((java.time.LocalDateTime) value);
                    }

                    field.set(entity, value);
                }
                list.add(entity);
            }
        } catch (Exception e) {
                AppLogger.getLogger().log(Level.SEVERE, "查询数据发生异常", e);
                throw new RuntimeException("查询数据发生异常",e);
        }finally{
            MyDataSource.releaseConnection(conn);
            BaseDAO.closeResource(null,ps,rs);
        }
        return list;
    }


    protected static void closeResource(Connection conn, PreparedStatement ps, java.sql.ResultSet rs ){
        try{
            if(ps != null) ps.close();
            if(rs != null) rs.close();
        }catch(SQLException e){
            AppLogger.getLogger().log(Level.SEVERE,"关闭资源发生异常",e);
        }

    }
}
