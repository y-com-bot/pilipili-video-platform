package com.yuan.utils;

import com.yuan.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;

/**
 * 这是 servlet 时代遗留下来的异常处理工具。
 *
 * application 现在已经主要走 springMVC 的全局异常处理，
 * 但保留这个类可以兼容少量非框架入口的场景。
 */
public class ExceptionHandler {
    public static void handle(Throwable e, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        if (e instanceof AppException appException) {
            AppLogger.getLogger().log(
                    Level.WARNING,
                    buildRequestMessage("兼容入口业务异常", req, appException.getMessage()),
                    appException
            );
            resp.setStatus(appException.getCode());
            resp.getWriter().println(JsonUtils.error(appException.getCode(), appException.getMessage()));
            return;
        }

        AppLogger.getLogger().log(
                Level.SEVERE,
                buildRequestMessage("兼容入口系统异常", req, e == null ? null : e.getMessage()),
                e
        );
        resp.setStatus(500);
        resp.getWriter().println(JsonUtils.error(500, "系统开小差了，请稍后再试"));
    }

    private static String buildRequestMessage(String prefix, HttpServletRequest request, String message) {
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

    private static String safeMessage(String message) {
        return message == null || message.trim().isEmpty() ? "无详细异常信息" : message.trim();
    }
}
