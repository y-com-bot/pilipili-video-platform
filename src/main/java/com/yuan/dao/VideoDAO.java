package com.yuan.dao;

import com.yuan.entity.Video;

import java.util.List;

public class VideoDAO extends BaseDAO{
    public boolean insertVideo(Video video){
        String sql = "INSERT INTO video(title, description, uploader_id, video_url) VALUES(?, ?, ?, ?)";
        int rows = update(sql, video.getTitle(), video.getDescription(), video.getUploaderId(), video.getVideoUrl());
        return rows > 0;
    }

    public List<Video> findAllVideos() {
        String sql = "SELECT id, title, description, uploader_id AS uploaderId, video_url AS videoUrl, like_count AS likeCount, create_time AS createTime FROM video ORDER BY create_time DESC";
        return queryForList(Video.class, sql);
    }

    public int countAllVideos() {
        String sql = "select count(*) as cnt from video";
        Object count = queryForValue(sql);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public int sumVideoLikes() {
        String sql = "select coalesce(sum(like_count),0) as cnt from video";
        Object count = queryForValue(sql);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public Video findVideoById(Long id){
        String sql = "SELECT id, title, description, uploader_id AS uploaderId, video_url AS videoUrl, like_count AS likeCount, create_time AS createTime FROM video WHERE id = ?";
        List<Video> videos = queryForList(Video.class, sql, id);
        return videos.isEmpty() ? null : videos.get(0);
    }

    public boolean deleteVideo(Long id) {
        String sql = "DELETE FROM video WHERE id = ?";
        int rows = update(sql, id);
        return rows > 0;
    }
}
