package com.yuan.controller;

import com.yuan.dao.CommentDAO;
import com.yuan.entity.Comment;
import com.yuan.exception.AppException;
import com.yuan.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import springMVC.MyAutowired;
import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyRequestParam;
import springMVC.MyResponseBody;

import java.util.List;

@MyController
@MyRequestMapping("/api/comments")
public class CommentServlet extends BaseApiController {

    @MyAutowired
    private CommentDAO commentDAO;

    @MyRequestMapping("/list")
    @MyResponseBody
    public String list(@MyRequestParam("videoId") Long videoId,
                       @MyRequestParam("targetType") Integer targetType,
                       @MyRequestParam("targetId") Long targetId,
                       @MyRequestParam("sort") String sort,
                       @MyRequestParam("pageSize") Integer pageSize) {
        Integer safeTargetType = resolveTargetType(videoId, targetType);
        Long safeTargetId = resolveTargetId(videoId, targetId);
        String safeSort = normalizeSort(sort);
        List<Comment> comments = "time".equals(safeSort)
                ? commentDAO.findByTarget(safeTargetType, safeTargetId, pageSize)
                : commentDAO.findByTargetOrderByHot(safeTargetType, safeTargetId, pageSize);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            json.append("{")
                    .append("\"id\":").append(comment.getId()).append(",")
                    .append("\"targetType\":").append(comment.getTargetType()).append(",")
                    .append("\"targetId\":").append(comment.getTargetId()).append(",")
                    .append("\"videoId\":").append(comment.getVideoId() == null ? "null" : comment.getVideoId()).append(",")
                    .append("\"userId\":").append(comment.getUserId()).append(",")
                    .append("\"content\":").append(jsonString(comment.getContent())).append(",")
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
                         @MyRequestParam("targetType") Integer targetType,
                         @MyRequestParam("targetId") Long targetId,
                         @MyRequestParam("content") String content,
                         HttpServletRequest request) {
        Long userId = requireLogin(request, "login required");
        Integer safeTargetType = resolveTargetType(videoId, targetType);
        Long safeTargetId = resolveTargetId(videoId, targetId);

        requireNonBlank(content, "comment content is required");

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetType(safeTargetType);
        comment.setTargetId(safeTargetId);
        comment.setVideoId(safeTargetType == 0 ? safeTargetId : null);
        comment.setContent(content.trim());

        boolean success = commentDAO.insertComment(comment);
        if (!success) {
            throw new AppException(500, "create comment failed");
        }
        return JsonUtils.success("comment created");
    }

    @MyRequestMapping("/delete")
    @MyResponseBody
    public String delete(@MyRequestParam("commentId") Long commentId, HttpServletRequest request) {
        Long userId = requireLogin(request, "login required");
        requireNotNull(commentId, "commentId is required");

        boolean success = commentDAO.deleteById(commentId, userId);
        if (!success) {
            throw new AppException(404, "comment not found or no permission");
        }
        return JsonUtils.success("comment deleted");
    }

    private Integer resolveTargetType(Long videoId, Integer targetType) {
        if (videoId != null) {
            return 0;
        }
        if (targetType == null || (targetType != 0 && targetType != 1)) {
            throw new AppException(400, "unsupported target type");
        }
        return targetType;
    }

    private Long resolveTargetId(Long videoId, Long targetId) {
        Long safeTargetId = videoId != null ? videoId : targetId;
        if (safeTargetId == null) {
            throw new AppException(400, "missing target id");
        }
        return safeTargetId;
    }

    private String normalizeSort(String sort) {
        String safeSort = trimToNull(sort);
        if (safeSort == null) {
            return "hot";
        }
        if ("hot".equalsIgnoreCase(safeSort) || "time".equalsIgnoreCase(safeSort)) {
            return safeSort.toLowerCase();
        }
        throw new AppException(400, "sort must be hot or time");
    }
}
