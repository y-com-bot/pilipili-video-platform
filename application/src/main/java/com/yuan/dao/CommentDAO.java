package com.yuan.dao;

import com.yuan.entity.Comment;
import com.yuan.utils.ConfigManager;
import springMVC.MyService;

import java.util.List;

@MyService
public class CommentDAO extends BaseDAO {

    private static final int DEFAULT_LIST_SIZE = Integer.parseInt(
            ConfigManager.getProperty("comment.list.default-size", "20")
    );
    private static final int MAX_LIST_SIZE = Integer.parseInt(
            ConfigManager.getProperty("comment.list.max-size", "100")
    );

    public boolean insertComment(Comment comment) {
        String sql = "insert into comment (target_type, target_id, user_id, content) values (?, ?, ?, ?)";
        return update(sql, comment.getTargetType(), comment.getTargetId(), comment.getUserId(), comment.getContent()) > 0;
    }

    public List<Comment> findByVideoId(Long videoId) {
        return findByTarget(0, videoId, DEFAULT_LIST_SIZE);
    }

    public List<Comment> findByTarget(Integer targetType, Long targetId) {
        return findByTarget(targetType, targetId, DEFAULT_LIST_SIZE);
    }

    public List<Comment> findByTarget(Integer targetType, Long targetId, Integer pageSize) {
        String sql = "select id, target_type as targetType, target_id as targetId, "
                + "case when target_type = 0 then target_id else null end as videoId, "
                + "user_id as userId, content, like_count as likeCount, create_time as createTime "
                + "from comment where target_type = ? and target_id = ? "
                + "order by create_time desc, id desc limit " + normalizeLimit(pageSize);
        return queryForList(Comment.class, sql, targetType, targetId);
    }

    public boolean deleteById(Long commentId, Long userId) {
        String sql = "delete from comment where id = ? and user_id = ?";
        return update(sql, commentId, userId) > 0;
    }

    public boolean deleteById(Long commentId) {
        String sql = "delete from comment where id = ?";
        return update(sql, commentId) > 0;
    }

    public int countAllComments() {
        String sql = "select count(*) as cnt from comment";
        Object count = queryForValue(sql);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public int sumCommentLikes() {
        String sql = "select coalesce(sum(like_count),0) as cnt from comment";
        Object count = queryForValue(sql);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public List<Comment> findByVideoIdOrderByHot(Long videoId) {
        return findByTargetOrderByHot(0, videoId, DEFAULT_LIST_SIZE);
    }

    public List<Comment> findByTargetOrderByHot(Integer targetType, Long targetId) {
        return findByTargetOrderByHot(targetType, targetId, DEFAULT_LIST_SIZE);
    }

    public List<Comment> findByTargetOrderByHot(Integer targetType, Long targetId, Integer pageSize) {
        String sql = "select id, target_type as targetType, target_id as targetId, "
                + "case when target_type = 0 then target_id else null end as videoId, "
                + "user_id as userId, content, like_count as likeCount, create_time as createTime "
                + "from comment where target_type = ? and target_id = ? "
                + "order by like_count desc, create_time desc, id desc limit " + normalizeLimit(pageSize);
        return queryForList(Comment.class, sql, targetType, targetId);
    }

    private int normalizeLimit(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_LIST_SIZE;
        }
        return Math.min(pageSize, MAX_LIST_SIZE);
    }
}
