package com.yuan.controller;

import com.yuan.service.LikeService;
import com.yuan.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import springMVC.MyAutowired;
import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyRequestParam;
import springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/likes")
public class LikeServlet extends BaseApiController {

    @MyAutowired
    private LikeService likeService;

    @MyRequestMapping("/toggle")
    @MyResponseBody
    public String toggle(@MyRequestParam("targetType") Integer targetType,
                         @MyRequestParam("targetId") Long targetId,
                         HttpServletRequest request) {
        Long userId = requireLogin(request, "login required");
        requireNotNull(targetType, "targetType is required");
        requireNotNull(targetId, "targetId is required");

        String message = likeService.toggleLike(userId, targetType, targetId);
        return JsonUtils.success(message);
    }
}
