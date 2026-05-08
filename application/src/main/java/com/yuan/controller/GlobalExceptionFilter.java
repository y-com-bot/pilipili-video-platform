package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.utils.AppLogger;
import com.yuan.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import springMVC.MyControllerAdvice;
import springMVC.MyExceptionHandler;
import springMVC.MyResponseBody;

import java.util.logging.Level;

/**
 * 这里虽然类名还叫 GlobalExceptionFilter，
 * 但角色已经改成 springMVC 的全局异常处理器。
 */
@MyControllerAdvice
public class GlobalExceptionFilter {

    @MyExceptionHandler(AppException.class)
    @MyResponseBody
    public String handleAppException(AppException e, HttpServletRequest request) {
        AppLogger.getLogger().log(
                Level.WARNING,
                buildRequestMessage("业务异常", request, e.getMessage()),
                e
        );
        return JsonUtils.error(e.getCode(), e.getMessage());
    }

    @MyExceptionHandler(Exception.class)
    @MyResponseBody
    public String handleException(Exception e, HttpServletRequest request) {
        AppLogger.getLogger().log(
                Level.SEVERE,
                buildRequestMessage("系统异常", request, e.getMessage()),
                e
        );
        return JsonUtils.error(500, "系统开小差了，请稍后再试");
    }

    private String buildRequestMessage(String prefix, HttpServletRequest request, String message) {
        if (request == null) {
            return prefix + " - " + safeMessage(message);
        }
        return prefix
                + " - "
                + request.getMethod()
                + " "
                + request.getRequestURI()
                + " - "
                + safeMessage(message);
    }

    private String safeMessage(String message) {
        return message == null || message.trim().isEmpty() ? "无详细异常信息" : message.trim();
    }
}
