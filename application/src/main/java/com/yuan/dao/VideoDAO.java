package com.yuan.dao;

import com.yuan.entity.Video;
import com.yuan.utils.ConfigManager;
import springMVC.MyService;

import java.util.List;

@MyService
public class VideoDAO extends BaseDAO {
    private static final int DEFAULT_LIST_SIZE = Integer.parseInt(
            ConfigManager.getProperty("video.list.default-size", "20")
    );
    private static final int MAX_LIST_SIZE = Integer.parseInt(
            ConfigManager.getProperty("video.list.max-size", "50")
    );

    public Long insertVideo(Video video) {
        String sql = "INSERT INTO video(title, description, uploader_id, category, video_url) VALUES(?, ?, ?, ?, ?)";
        return insertAndReturnId(sql,
                video.getTitle(),
                video.getDescription(),
                video.getUploaderId(),
                video.getCategory(),
                video.getVideoUrl());
    }

    public List<Video> findAllVideos() {
        return findLatestVideos(DEFAULT_LIST_SIZE);
    }

    public List<Video> findLatestVideos(Integer pageSize) {
        int limit = normalizeLimit(pageSize);
        String sql = "SELECT v.id, v.title, v.description, v.uploader_id AS uploaderId, u.username as uploaderName, "
                + "v.category, v.video_url AS videoUrl, v.like_count AS likeCount, v.create_time AS createTime, "
                + "(select count(*) from comment c where c.target_type = 0 and c.target_id = v.id) as commentCount "
                + "FROM video v left join user u on u.id = v.uploader_id "
                + "ORDER BY v.create_time DESC, v.id DESC limit " + limit;
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

    public Video findVideoById(Long id) {
        String sql = "SELECT v.id, v.title, v.description, v.uploader_id AS uploaderId, u.username as uploaderName, "
                + "v.category, v.video_url AS videoUrl, v.like_count AS likeCount, v.create_time AS createTime, "
                + "(select count(*) from comment c where c.target_type = 0 and c.target_id = v.id) as commentCount "
                + "FROM video v left join user u on u.id = v.uploader_id WHERE v.id = ?";
        List<Video> videos = queryForList(Video.class, sql, id);
        return videos.isEmpty() ? null : videos.get(0);
    }

    public boolean deleteVideo(Long id) {
        String sql = "DELETE FROM video WHERE id = ?";
        return update(sql, id) > 0;
    }

    private int normalizeLimit(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_LIST_SIZE;
        }
        return Math.min(pageSize, MAX_LIST_SIZE);
    }
}
