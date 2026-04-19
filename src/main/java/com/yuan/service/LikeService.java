package com.yuan.service;

import com.yuan.dao.LikeDAO;
import com.yuan.exception.AppException;
import com.yuan.utils.AppLogger;
import java.util.logging.Level;

public class LikeService {
    private LikeDAO likeDAO = new LikeDAO();

    public String toggleLike(Long userId, Integer targetType, Long targetId) {
        try {
            boolean isLike = likeDAO.checkIsLiked(userId, targetType, targetId);

            if (isLike) {
                likeDAO.removeLikeRecord(userId, targetType, targetId);
                likeDAO.updateLikeCount(targetType, targetId, -1);
                AppLogger.getLogger().info("用户 " + userId + " 取消了对 " + 
                    (targetType == 0 ? "视频" : "评论") + " " + targetId + " 的点赞");
                return "取消点赞成功";
            } else {
                likeDAO.addLikeRecord(userId, targetType, targetId);
                likeDAO.updateLikeCount(targetType, targetId, 1);
                AppLogger.getLogger().info("用户 " + userId + " 点赞了 " + 
                    (targetType == 0 ? "视频" : "评论") + " " + targetId);
                return "点赞成功";
            }
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "点赞操作失败: userId=" + userId + 
                ", targetType=" + targetType + ", targetId=" + targetId, e);
            throw new AppException(500, "点赞操作失败，请稍后重试");
        }
    }
}
