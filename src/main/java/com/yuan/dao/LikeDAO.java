package com.yuan.dao;

import com.yuan.utils.AppLogger;
import java.util.logging.Level;

public class LikeDAO extends BaseDAO {

    public boolean checkIsLiked(Long userId, Integer targetType, Long targetId) {
        try {
            String sql = "select count(*) as cnt from user_like where user_id = ? and target_type = ? and target_id = ?";
            java.sql.Connection conn = com.yuan.utils.MyDataSource.getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setObject(1, userId);
            ps.setObject(2, targetType);
            ps.setObject(3, targetId);
            java.sql.ResultSet rs = ps.executeQuery();
            boolean result = false;
            if (rs.next()) {
                int count = rs.getInt("cnt");
                result = count > 0;
            }
            rs.close();
            ps.close();
            com.yuan.utils.MyDataSource.releaseConnection(conn);
            return result;
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "检查点赞状态失败", e);
            return false;
        }
    }

    public void addLikeRecord(Long userId, Integer targetType, Long targetId) {
        String sql = "insert into user_like(user_id, target_type, target_id) values (?, ?, ?)";
        update(sql, userId, targetType, targetId);
    }

    public void removeLikeRecord(Long userId, Integer targetType, Long targetId) {
        String sql = "delete from user_like where user_id = ? and target_type = ? and target_id = ?";
        update(sql, userId, targetType, targetId);
    }

    public void updateLikeCount(Integer targetType, Long targetId, int increment) {
        String tableName = (targetType == 0) ? "video" : "comment";
        String sql = "update " + tableName + " set like_count = like_count + ? where id = ?";
        update(sql, increment, targetId);
    }
}
