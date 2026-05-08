package com.yuan.dao;

import springMVC.MyService;

@MyService
public class LikeDAO extends BaseDAO {

    public boolean checkIsLiked(Long userId, Integer targetType, Long targetId) {
        String sql = "select count(*) as cnt from user_like where user_id = ? and target_type = ? and target_id = ?";
        Object count = queryForValue(sql, userId, targetType, targetId);
        return count != null && ((Number) count).intValue() > 0;
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
        String tableName = switch (targetType) {
            case 0 -> "video";
            case 1 -> "comment";
            case 2 -> "dynamic_post";
            default -> throw new IllegalArgumentException("unsupported like target type: " + targetType);
        };
        String sql = "update " + tableName + " set like_count = like_count + ? where id = ?";
        update(sql, increment, targetId);
    }
}
