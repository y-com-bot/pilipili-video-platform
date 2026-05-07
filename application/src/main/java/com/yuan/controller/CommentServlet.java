package com.yuan.controller;

import com.yuan.dao.CommentDAO;
import com.yuan.entity.Comment;
import com.yuan.exception.AppException;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@MyController
@MyRequestMapping("/api/comments")
public class CommentServlet {

    @MyAutowired
    private CommentDAO commentDAO;

    @MyRequestMapping("/list")
    @MyResponseBody
    public String list(@MyRequestParam("videoId") Long videoId) {
        if (videoId == null) {
            throw new AppException(400, "缺少视频 ID");
        }

        List<Comment> comments = commentDAO.findByVideoIdOrderByHot(videoId);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            json.append("{")
                    .append("\"id\":").append(comment.getId()).append(",")
                    .append("\"videoId\":").append(comment.getVideoId()).append(",")
                    .append("\"userId\":").append(comment.getUserId()).append(",")
                    .append("\"content\":\"").append(JsonUtils.escape(comment.getContent())).append("\",")
                    .append("\"likeCount\":").append(comment.getLikeCount() == null ? 0 : comment.getLikeCount())
                    .append("}");
            if (i < comments.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    @MyRequestMapping("/create")
    @MyResponseBody
    public String create(@MyRequestParam("videoId") Long videoId,
                         @MyRequestParam("content") String content,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AppException(401, "请先登录后再发表评论");
        }
        if (videoId == null || content == null || content.trim().isEmpty()) {
            throw new AppException(400, "评论内容不能为空");
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setVideoId(videoId);
        comment.setContent(content.trim());

        boolean success = commentDAO.insertComment(comment);
        if (!success) {
            throw new AppException(500, "评论发布失败，请稍后再试");
        }
        return JsonUtils.success("评论发布成功");
    }
}
