package com.yuan.controller;

import com.yuan.entity.Video;
import com.yuan.exception.AppException;
import com.yuan.service.UserBehaviorCacheService;
import com.yuan.service.VideoService;
import com.yuan.utils.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import springMVC.MyAutowired;
import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyRequestParam;
import springMVC.MyResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@MyController
@MyRequestMapping("/api/videos")
public class VideoServlet extends BaseApiController {

    @MyAutowired
    private VideoService videoService;

    @MyAutowired
    private UserBehaviorCacheService userBehaviorCacheService;

    @MyRequestMapping("/list")
    @MyResponseBody
    public String list(@MyRequestParam("pageSize") Integer pageSize, HttpServletRequest request) {
        Long currentUserId = currentUserId(request);
        List<Video> videoList = videoService.getVideoList(pageSize);
        Set<Long> followeeIds = currentUserId == null ? Set.of() : userBehaviorCacheService.getFolloweeIds(currentUserId);
        Set<Long> favoriteVideoIds = currentUserId == null ? Set.of() : userBehaviorCacheService.getFavoriteTargetIds(currentUserId, 0);

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < videoList.size(); i++) {
            Video video = videoList.get(i);
            json.append(toVideoJson(
                    video,
                    followeeIds.contains(video.getUploaderId()),
                    favoriteVideoIds.contains(video.getId())
            ));
            if (i < videoList.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    @MyRequestMapping("/detail")
    @MyResponseBody
    public String detail(@MyRequestParam("id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new AppException(400, "missing video id");
        }

        Video video = videoService.getVideoById(id);
        if (video == null) {
            throw new AppException(404, "video not found");
        }
        Long currentUserId = currentUserId(request);
        boolean followed = currentUserId != null && userBehaviorCacheService.getFolloweeIds(currentUserId).contains(video.getUploaderId());
        boolean favorited = currentUserId != null && userBehaviorCacheService.getFavoriteTargetIds(currentUserId, 0).contains(video.getId());
        return toVideoJson(video, followed, favorited);
    }

    @MyRequestMapping("/publish")
    @MyResponseBody
    public String publish(HttpServletRequest request) {
        Long uploaderId = requireLogin(request, "login required");

        String title = trimToNull(request.getParameter("title"));
        String description = trimToNull(request.getParameter("description"));
        String category = trimToNull(request.getParameter("category"));
        String videoUrl = trimToNull(request.getParameter("videoUrl"));
        Part videoFile = getVideoFilePart(request);

        if (title == null) {
            throw new AppException(400, "video title is required");
        }
        if (category == null) {
            throw new AppException(400, "video category is required");
        }

        return JsonUtils.success(videoService.publishVideo(
                title,
                description,
                category,
                resolveVideoUrl(videoUrl, videoFile, request),
                uploaderId
        ));
    }

    private String resolveVideoUrl(String videoUrl, Part videoFile, HttpServletRequest request) {
        if (hasUploadedFile(videoFile)) {
            return saveUploadedVideo(videoFile, request);
        }
        if (videoUrl == null) {
            throw new AppException(400, "video url or file is required");
        }
        return videoUrl;
    }

    private Part getVideoFilePart(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return null;
        }

        try {
            return request.getPart("videoFile");
        } catch (IOException | ServletException e) {
            throw new AppException(400, "parse uploaded video failed");
        }
    }

    private boolean hasUploadedFile(Part videoFile) {
        return videoFile != null
                && videoFile.getSize() > 0
                && trimToNull(videoFile.getSubmittedFileName()) != null;
    }

    private String saveUploadedVideo(Part videoFile, HttpServletRequest request) {
        validateVideoFile(videoFile);

        String originalFileName = Paths.get(videoFile.getSubmittedFileName()).getFileName().toString();
        String extension = resolveFileExtension(originalFileName, videoFile.getContentType());
        Path uploadDir = resolveUploadDir(request);
        String storedFileName = System.currentTimeMillis()
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = videoFile.getInputStream()) {
                Files.copy(inputStream, uploadDir.resolve(storedFileName));
            }
        } catch (IOException e) {
            throw new AppException(500, "save uploaded video failed");
        }

        return request.getContextPath() + "/uploads/videos/" + storedFileName;
    }

    private void validateVideoFile(Part videoFile) {
        String extension = resolveFileExtension(videoFile.getSubmittedFileName(), videoFile.getContentType());
        if (!(".mp4".equals(extension)
                || ".webm".equals(extension)
                || ".ogg".equals(extension)
                || ".mov".equals(extension))) {
            throw new AppException(400, "unsupported video file type");
        }
    }

    private String resolveFileExtension(String originalFileName, String contentType) {
        String fileName = trimToNull(originalFileName);
        if (fileName != null) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }

        if (contentType == null) {
            throw new AppException(400, "unknown video content type");
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/ogg" -> ".ogg";
            case "video/quicktime" -> ".mov";
            default -> throw new AppException(400, "unknown video content type");
        };
    }

    private Path resolveUploadDir(HttpServletRequest request) {
        String realPath = request.getServletContext().getRealPath("/uploads/videos");
        if (realPath == null || realPath.trim().isEmpty()) {
            throw new AppException(500, "upload directory unavailable");
        }
        return Paths.get(realPath);
    }

    private String toVideoJson(Video video, boolean followed, boolean favorited) {
        return "{"
                + "\"id\":" + video.getId() + ","
                + "\"title\":" + jsonString(video.getTitle()) + ","
                + "\"description\":" + jsonString(video.getDescription()) + ","
                + "\"uploaderId\":" + (video.getUploaderId() == null ? "null" : video.getUploaderId()) + ","
                + "\"uploaderName\":" + jsonString(video.getUploaderName()) + ","
                + "\"category\":" + jsonString(video.getCategory()) + ","
                + "\"videoUrl\":" + jsonString(video.getVideoUrl()) + ","
                + "\"likeCount\":" + (video.getLikeCount() == null ? 0 : video.getLikeCount()) + ","
                + "\"commentCount\":" + (video.getCommentCount() == null ? 0 : video.getCommentCount()) + ","
                + "\"followed\":" + followed + ","
                + "\"favorited\":" + favorited
                + "}";
    }
}
