package com.yuan.service;

import com.yuan.dao.LikeDAO;
import com.yuan.exception.AppException;
import framework.springMVC.MyAutowired;
import framework.springMVC.MyService;

@MyService
public class LikeService {

    @MyAutowired
    private LikeDAO likeDAO;

    public String toggleLike(Long userId, Integer targetType, Long targetId) {
        try {
            boolean liked = likeDAO.checkIsLiked(userId, targetType, targetId);
            if (liked) {
                likeDAO.removeLikeRecord(userId, targetType, targetId);
                likeDAO.updateLikeCount(targetType, targetId, -1);
                return "已取消点赞";
            }

            likeDAO.addLikeRecord(userId, targetType, targetId);
            likeDAO.updateLikeCount(targetType, targetId, 1);
            return "点赞成功";
        } catch (Exception e) {
            throw new AppException(500, "点赞操作失败，请稍后再试");
        }
    }
}
