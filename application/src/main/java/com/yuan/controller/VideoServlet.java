package com.yuan.controller;

import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import com.yuan.service.VideoService;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@MyController
@MyRequestMapping("/api/videos")
public class VideoServlet {

    @MyAutowired
    private VideoService videoService;

    @MyRequestMapping("/list")
    @MyResponseBody
    public String list() {
        List<Video> videoList = videoService.getVideoList();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < videoList.size(); i++) {
            json.append(toVideoJson(videoList.get(i)));
            if (i < videoList.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    @MyRequestMapping("/detail")
    @MyResponseBody
    public String detail(@MyRequestParam("id") Long id) {
        if (id == null) {
            throw new AppException(400, "缺少视频 ID");
        }

        Video video = videoService.getVideoById(id);
        if (video == null) {
            throw new AppException(404, "视频不存在");
        }
        return toVideoJson(video);
    }

    @MyRequestMapping("/publish")
    @MyResponseBody
    public String publish(@MyRequestParam("title") String title,
                          @MyRequestParam("description") String description,
                          @MyRequestParam("videoUrl") String videoUrl,
                          HttpServletRequest request) {
        Long uploaderId = (Long) request.getAttribute("currentUserId");
        if (uploaderId == null) {
            throw new AppException(401, "请先登录后再上传视频");
        }
        if (title == null || title.trim().isEmpty() || videoUrl == null || videoUrl.trim().isEmpty()) {
            throw new AppException(400, "标题和视频地址不能为空");
        }

        return JsonUtils.success(videoService.publishVideo(title.trim(), description, videoUrl.trim(), uploaderId));
    }

    private String toVideoJson(Video video) {
        return "{"
                + "\"id\":" + video.getId() + ","
                + "\"title\":\"" + JsonUtils.escape(video.getTitle()) + "\","
                + "\"description\":\"" + JsonUtils.escape(video.getDescription()) + "\","
                + "\"uploaderId\":" + (video.getUploaderId() == null ? "null" : video.getUploaderId()) + ","
                + "\"videoUrl\":\"" + JsonUtils.escape(video.getVideoUrl()) + "\","
                + "\"likeCount\":" + (video.getLikeCount() == null ? 0 : video.getLikeCount())
                + "}";
    }
}
