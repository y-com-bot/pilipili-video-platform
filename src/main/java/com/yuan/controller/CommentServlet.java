package com.yuan.controller;

import com.yuan.dao.CommentDAO;
import com.yuan.entity.Comment;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/comment")
public class CommentServlet extends HttpServlet {

    private CommentDAO commentDAO = new CommentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        Long userId = (Long) req.getAttribute("currentUserId");

        String videoIdStr = req.getParameter("videoId");
        String content = req.getParameter("content");

        if (videoIdStr == null || content == null || content.trim().isEmpty()) {
            resp.getWriter().write("评论内容不能为空！");
            return;
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setVideoId(Long.parseLong(videoIdStr));
        comment.setContent(content);

        boolean success = commentDAO.insertComment(comment);
        resp.getWriter().write(success ? "评论成功！" : "评论失败。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        resp.setContentType("application/json;charset=UTF-8");

        String videoIdStr = req.getParameter("videoId");
        if (videoIdStr == null) return;

        List<Comment> comments = commentDAO.findByVideoId(Long.parseLong(videoIdStr));

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < comments.size(); i++) {
            Comment c = comments.get(i);

            String safeContent = "";
            if (c.getContent() != null) {
                safeContent = c.getContent()
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "");
            }

            json.append("{")
                    .append("\"id\":").append(c.getId()).append(",")
                    .append("\"userId\":").append(c.getUserId()).append(",")
                    .append("\"content\":\"").append(safeContent).append("\",")
                    .append("\"likeCount\":").append(c.getLikeCount() != null ? c.getLikeCount() : 0)
                    .append("}");

            if (i < comments.size() - 1) json.append(",");
        }
        json.append("]");

        resp.getWriter().write(json.toString());
    }
}