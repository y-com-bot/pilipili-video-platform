package com.yuan.service;

import com.yuan.dao.VideoDAO;
import com.yuan.entity.Video;

import java.util.List;

public class VideoService {
    VideoDAO videoDAO = new VideoDAO();
    public String publishVideo(String title, String description, String videoUrl, Long uploaderId){
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setUploaderId(uploaderId);
        video.setVideoUrl(videoUrl);
        boolean ifsuccess = videoDAO.insertVideo(video);
        return ifsuccess ? "视频发布成功" : "视频发布失败,请稍后再试";
    }

    public List<Video> getVideoList(){

        return videoDAO.findAllVideos();
    }
}
