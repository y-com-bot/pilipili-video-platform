package com.yuan.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yuan.entity.Video;
import com.yuan.service.VideoService;
import com.yuan.utils.JwtUtils;
import com.yuan.utils.TokenCache;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/video")
public class VideoServlet extends HttpServlet {
    private VideoService videoService = new VideoService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        List<Video> videoList = videoService.getVideoList();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < videoList.size(); i++) {
            Video v = videoList.get(i);
            json.append("{")
                    .append("\"id\":").append(v.getId()).append(",")
                    .append("\"title\":\"").append(v.getTitle()).append("\",")
                    .append("\"description\":\"").append(v.getDescription() != null ? v.getDescription() : "").append("\",")
                    .append("\"videoUrl\":\"").append(v.getVideoUrl()).append("\",")
                    .append("\"likeCount\":").append(v.getLikeCount() != null ? v.getLikeCount() : 0)
                    .append("}");
            if (i < videoList.size() - 1) json.append(",");
        }
        json.append("]");

        resp.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        // 从请求中获取当前用户ID（由LoginFilter设置）
        Long uploaderId = (Long) req.getAttribute("currentuserId");
        String title = req.getParameter("title");
        String videoUrl = req.getParameter("videoUrl");
        String description = req.getParameter("description");

        if (title == null || title.trim().isEmpty() || videoUrl == null || videoUrl.trim().isEmpty()) {
            resp.getWriter().write("标题和视频地址不能为空");
            return;
        }

        String result = videoService.publishVideo(title, description, videoUrl, uploaderId);
        resp.getWriter().write(result);
    }
}