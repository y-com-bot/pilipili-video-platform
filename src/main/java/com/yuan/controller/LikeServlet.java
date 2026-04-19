package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.service.LikeService;
import com.yuan.utils.AppLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;

@WebServlet("/like")
public class LikeServlet extends HttpServlet {
    private LikeService likeService = new LikeService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        Long userId = (Long) req.getAttribute("currentUserId");
        if (userId == null) {
            throw new AppException(401, "未登录，请登录获取token");
        }

        String typeStr = req.getParameter("targetType");
        String idStr = req.getParameter("targetId");

        if (typeStr == null || idStr == null || typeStr.isBlank() || idStr.isBlank()) {
            throw new AppException(400, "参数错误");
        }

        try {
            Integer targetType = Integer.parseInt(typeStr);
            Long targetId = Long.parseLong(idStr);
            
            String result = likeService.toggleLike(userId, targetType, targetId);

            String json = "{\"code\":200,\"message\":\"" + result + "\",\"success\":true}";
            resp.getWriter().println(json);
            
        } catch (NumberFormatException e) {
            AppLogger.getLogger().log(Level.WARNING, "参数格式错误", e);
            throw new AppException(400, "参数格式错误，请检查 targetType 和 targetId");
        }
    }
}
