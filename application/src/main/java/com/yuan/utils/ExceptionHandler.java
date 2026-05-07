package com.yuan.utils;

import com.yuan.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 这是 servlet 时代遗留下来的异常处理工具。
 *
 * application 现在已经主要走 framework.springMVC 的全局异常处理，
 * 但保留这个类可以兼容少量非框架入口的场景，也方便后续排查问题。
 */
public class ExceptionHandler {
    public static void handle(Throwable e, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        if (e instanceof AppException appException) {
            resp.setStatus(appException.getCode());
            resp.getWriter().println(JsonUtils.error(appException.getCode(), appException.getMessage()));
            return;
        }

        resp.setStatus(500);
        resp.getWriter().println(JsonUtils.error(500, "系统开小差了，请稍后再试"));
    }
}
