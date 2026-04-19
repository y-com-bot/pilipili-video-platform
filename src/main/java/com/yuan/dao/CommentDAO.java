package com.yuan.dao;

import com.yuan.entity.Comment;
import com.yuan.utils.AppLogger;

import java.util.List;

public class CommentDAO extends BaseDAO{
    public boolean insertComment(Comment comment){
        String sql = "insert into comment (video_id, user_id, content) values (?, ?, ?)";
        return update(sql, comment.getVideoId(),comment.getUserId(),comment.getContent()) > 0;
    }

    public List<Comment> findByVideoId(Long videoId){
        String sql = "select id, video_id as videoId, user_id as userId, content, like_count as likeCount, create_time as createTime from comment where video_id = ?";
        return queryForList(Comment.class, sql, videoId);
    }

    public boolean deleteById(Long commentId, Long userId){
        String sql = "delete from comment where id = ? and user_id = ?";
        return update(sql, commentId, userId) > 0;
    }

    public boolean deleteById(Long commentId){
        String sql = "delete from comment where id = ?";
        return update(sql, commentId) > 0;
    }

    public int countAllComments() {
        String sql = "select count(*) as cnt from comment";
        try (java.sql.Connection conn = com.yuan.utils.MyDataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (java.sql.SQLException e) {
            AppLogger.getLogger().log(java.util.logging.Level.SEVERE, "统计评论总数失败", e);
        }
        return 0;
    }

    public int sumCommentLikes() {
        String sql = "select coalesce(sum(like_count),0) as cnt from comment";
        try (java.sql.Connection conn = com.yuan.utils.MyDataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (java.sql.SQLException e) {
            AppLogger.getLogger().log(java.util.logging.Level.SEVERE, "统计评论点赞数失败", e);
        }
        return 0;
    }

    public List<Comment> findByVideoIdOrderByHot(Long videoId){
        String sql = "select id, video_id as videoId, user_id as userId, content, like_count as likeCount, create_time as createTime " +
                "from comment where video_id = ? order by like_count desc, create_time desc";
        return queryForList(Comment.class, sql , videoId);
    }
}