package com.yuan.service;

import com.yuan.dao.VideoDAO;
import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyService;
import java.util.List;

@MyService
public class VideoService {

    @MyAutowired
    private VideoDAO videoDAO;

    public String publishVideo(String title, String description, String videoUrl, Long uploaderId) {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUploaderId(uploaderId);
        video.setVideoUrl(videoUrl);

        boolean success = videoDAO.insertVideo(video);
        if (!success) {
            throw new AppException(500, "视频发布失败，请稍后再试");
        }
        return "视频发布成功";
    }

    public List<Video> getVideoList() {
        return videoDAO.findAllVideos();
    }

    public Video getVideoById(Long id) {
        return videoDAO.findVideoById(id);
    }
}
