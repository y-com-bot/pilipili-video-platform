package com.yuan.controller;

import com.yuan.dao.CommentDAO;
import com.yuan.dao.UserDAO;
import com.yuan.dao.VideoDAO;
import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;
import java.util.List;

@MyController
@MyRequestMapping("/api/admin")
public class AdminServlet {

    @MyAutowired
    private VideoDAO videoDAO;

    @MyAutowired
    private CommentDAO commentDAO;

    @MyAutowired
    private UserDAO userDAO;

    @MyRequestMapping("/list")
    @MyResponseBody
    public String listVideos() {
        List<Video> videos = videoDAO.findAllVideos();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);
            json.append("{")
                    .append("\"id\":").append(video.getId()).append(",")
                    .append("\"title\":\"").append(JsonUtils.escape(video.getTitle())).append("\",")
                    .append("\"description\":\"").append(JsonUtils.escape(video.getDescription())).append("\",")
                    .append("\"uploaderId\":").append(video.getUploaderId()).append(",")
                    .append("\"likeCount\":").append(video.getLikeCount() == null ? 0 : video.getLikeCount())
                    .append("}");
            if (i < videos.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    @MyRequestMapping("/stats")
    @MyResponseBody
    public String stats() {
        int totalVideos = videoDAO.countAllVideos();
        int totalComments = commentDAO.countAllComments();
        int totalUsers = userDAO.countAllUsers();
        int totalLikes = videoDAO.sumVideoLikes() + commentDAO.sumCommentLikes();

        return "{"
                + "\"totalVideos\":" + totalVideos + ","
                + "\"totalComments\":" + totalComments + ","
                + "\"totalUsers\":" + totalUsers + ","
                + "\"totalLikes\":" + totalLikes
                + "}";
    }

    @MyRequestMapping("/deleteVideo")
    @MyResponseBody
    public String deleteVideo(@MyRequestParam("videoId") Long videoId) {
        if (videoId == null) {
            throw new AppException(400, "缺少视频 ID");
        }

        if (!videoDAO.deleteVideo(videoId)) {
            throw new AppException(404, "视频不存在或删除失败");
        }
        return JsonUtils.success("视频删除成功");
    }

    @MyRequestMapping("/deleteComment")
    @MyResponseBody
    public String deleteComment(@MyRequestParam("commentId") Long commentId) {
        if (commentId == null) {
            throw new AppException(400, "缺少评论 ID");
        }

        if (!commentDAO.deleteById(commentId)) {
            throw new AppException(404, "评论不存在或删除失败");
        }
        return JsonUtils.success("评论删除成功");
    }
}
