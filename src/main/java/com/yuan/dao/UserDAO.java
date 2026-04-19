package com.yuan.dao;

import com.yuan.entity.User;
import com.yuan.utils.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class UserDAO extends BaseDAO {
    public User findByUsername(String username) {
        String sql = "select id, username, password, salt, role, create_time as createTime from user where username = ?";
        List<User> list = queryForList(User.class, sql, username);
        return list.isEmpty() ? null : list.get(0);
    }

    public int countByRole(String role) {
        String sql = "select count(*) as cnt from user where role = ?";
        try (Connection conn = com.yuan.utils.MyDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "统计角色数量失败", e);
        }
        return 0;
    }

    public int countAllUsers() {
        String sql = "select count(*) as cnt from user";
        try (Connection conn = com.yuan.utils.MyDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            AppLogger.getLogger().log(Level.SEVERE, "统计用户总数失败", e);
        }
        return 0;
    }

    public boolean insertUser(User user){
        String sql = "insert into user(username, password, salt, role) values(?,?,?,?)";
        int rows = update(sql,user.getUsername(), user.getPassword(), user.getSalt(), user.getRole());
        return rows > 0;
    }
}
