package com.yuan.controller;

import com.yuan.dao.CommentDAO;
import com.yuan.dao.VideoDAO;
import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import com.yuan.utils.AppLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {
    private VideoDAO videoDAO = new VideoDAO();
    private CommentDAO commentDAO = new CommentDAO();
    private com.yuan.dao.UserDAO userDAO = new com.yuan.dao.UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            String servletPath = req.getServletPath();
            String requestURI = req.getRequestURI();
            String contextPath = req.getContextPath();
            pathInfo = requestURI.substring(contextPath.length() + servletPath.length());
        }

        if ("/list".equals(pathInfo)) {
            try {
                List<Video> videos = videoDAO.findAllVideos();
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < videos.size(); i++) {
                    Video video = videos.get(i);
                    jsonBuilder.append("{")
                            .append("\"id\":").append(video.getId()).append(",")
                            .append("\"title\":\"").append(video.getTitle().replace("\"", "\\\"")).append("\"").append(",")
                            .append("\"uploaderId\":").append(video.getUploaderId()).append(",")
                            .append("\"uploaderName\":\"\"").append("\"").append(",")
                            .append("\"likeCount\":").append(video.getLikeCount() == null ? 0 : video.getLikeCount())
                            .append("}");
                    if (i < videos.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");
                resp.getWriter().println(jsonBuilder.toString());
            } catch (Exception e) {
                AppLogger.getLogger().log(Level.SEVERE, "获取视频列表失败", e);
                throw new AppException(500, "获取视频列表失败");
            }
        } else if ("/stats".equals(pathInfo)) {
            try {
                int totalVideos = videoDAO.countAllVideos();
                int totalComments = commentDAO.countAllComments();
                int totalUsers = userDAO.countAllUsers();
                int totalLikes = videoDAO.sumVideoLikes() + commentDAO.sumCommentLikes();

                String json = String.format("{\"totalVideos\":%d,\"totalComments\":%d,\"totalUsers\":%d,\"totalLikes\":%d}",
                        totalVideos, totalComments, totalUsers, totalLikes);
                resp.getWriter().println(json);
            } catch (Exception e) {
                AppLogger.getLogger().log(Level.SEVERE, "获取统计数据失败", e);
                throw new AppException(500, "获取统计数据失败");
            }
        } else {
            throw new AppException(404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();
        Long adminUserId = (Long) req.getAttribute("currentUserId");
        String role = (String) req.getAttribute("currentRole");

        if (!"admin".equalsIgnoreCase(role)) {
            throw new AppException(403, "您无权执行此操作");
        }

        if ("/deleteVideo".equals(pathInfo)) {
            String videoIdStr = req.getParameter("videoId");
            if (videoIdStr == null || videoIdStr.isBlank()) {
                throw new AppException(400, "视频ID不能为空");
            }

            try {
                Long videoId = Long.parseLong(videoIdStr);
                videoDAO.deleteVideo(videoId);
                AppLogger.getLogger().info("管理员 " + adminUserId + " 删除了视频 " + videoId);
                
                String json = "{\"code\":200,\"message\":\"已成功删除视频\",\"success\":true}";
                resp.getWriter().println(json);
            } catch (NumberFormatException e) {
                throw new AppException(400, "视频ID格式错误");
            }
        } else if ("/deleteComment".equals(pathInfo)) {
            String commentIdStr = req.getParameter("commentId");
            if (commentIdStr == null || commentIdStr.isBlank()) {
                throw new AppException(400, "评论ID不能为空");
            }

            try {
                Long commentId = Long.parseLong(commentIdStr);
                commentDAO.deleteById(commentId);
                AppLogger.getLogger().info("管理员 " + adminUserId + " 删除了评论 " + commentId);

                String json = "{\"code\":200,\"message\":\"已成功删除评论\",\"success\":true}";
                resp.getWriter().println(json);
            } catch (NumberFormatException e) {
                throw new AppException(400, "评论ID格式错误");
            }
        } else {
            throw new AppException(404, "接口不存在");
        }
    }
}
