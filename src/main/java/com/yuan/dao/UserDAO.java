package com.yuan.dao;

import com.yuan.entity.User;

import java.util.List;

public class UserDAO extends BaseDAO {
    public User findByUsername(String username) {
        String sql = "select id, username, password, salt, role, create_time as createTime from user where username = ?";
        List<User> list = queryForList(User.class, sql, username);
        return list.isEmpty() ? null : list.get(0);
    }

    public int countByRole(String role) {
        String sql = "select count(*) as cnt from user where role = ?";
        Object count = queryForValue(sql, role);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public int countAllUsers() {
        String sql = "select count(*) as cnt from user";
        Object count = queryForValue(sql);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public boolean insertUser(User user){
        String sql = "insert into user(username, password, salt, role) values(?,?,?,?)";
        int rows = update(sql,user.getUsername(), user.getPassword(), user.getSalt(), user.getRole());
        return rows > 0;
    }
}
