package com.yuan.service;

import com.yuan.dao.FeedPushTaskDAO;
import com.yuan.dao.VideoDAO;
import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import com.yuan.utils.MyDataSource;
import springMVC.MyAutowired;
import springMVC.MyService;

import java.util.List;

@MyService
public class VideoService {

    @MyAutowired
    private VideoDAO videoDAO;

    @MyAutowired
    private FeedPushTaskDAO feedPushTaskDAO;

    @MyAutowired
    private FeedPushDispatcher feedPushDispatcher;

    public String publishVideo(String title, String description, String category, String videoUrl, Long uploaderId) {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setCategory(category);
        video.setUploaderId(uploaderId);
        video.setVideoUrl(videoUrl);

        Long taskId = null;
        try {
            MyDataSource.beginTransaction();

            Long videoId = videoDAO.insertVideo(video);
            if (videoId == null) {
                throw new AppException(500, "video publish failed");
            }

            taskId = feedPushTaskDAO.insertTask(uploaderId, "video", videoId);
            if (taskId == null) {
                throw new AppException(500, "feed push task create failed");
            }

            MyDataSource.commitTransaction();
        } catch (AppException e) {
            MyDataSource.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            MyDataSource.rollbackTransaction();
            throw new AppException(500, "video publish failed");
        } finally {
            MyDataSource.endTransaction();
        }

        feedPushDispatcher.notifyTask(taskId);
        return "video published";
    }

    public List<Video> getVideoList(Integer pageSize) {
        return videoDAO.findLatestVideos(pageSize);
    }

    public List<Video> getVideoList() {
        return videoDAO.findAllVideos();
    }

    public Video getVideoById(Long id) {
        return videoDAO.findVideoById(id);
    }
}
