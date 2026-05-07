package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.service.LikeService;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyRequestParam;
import framework.springMVC.MyResponseBody;
import jakarta.servlet.http.HttpServletRequest;

@MyController
@MyRequestMapping("/api/likes")
public class LikeServlet {

    @MyAutowired
    private LikeService likeService;

    @MyRequestMapping("/toggle")
    @MyResponseBody
    public String toggle(@MyRequestParam("targetType") Integer targetType,
                         @MyRequestParam("targetId") Long targetId,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AppException(401, "请先登录后再点赞");
        }
        if (targetType == null || targetId == null) {
            throw new AppException(400, "点赞参数不完整");
        }

        String message = likeService.toggleLike(userId, targetType, targetId);
        return JsonUtils.success(message);
    }
}
