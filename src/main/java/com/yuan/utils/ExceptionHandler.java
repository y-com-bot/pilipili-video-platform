package com.yuan.utils;

import com.yuan.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;

public class ExceptionHandler {
    public static void handle(Throwable e, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");

        String contentType = resp.getContentType();
        String accept = req.getHeader("Accept");
        String xRequestedWith = req.getHeader("X-Requested-With");
        boolean isJson = (contentType != null && contentType.contains("application/json"))
                || (accept != null && accept.contains("application/json"))
                || (xRequestedWith != null && "XMLHttpRequest".equalsIgnoreCase(xRequestedWith));

        if (e instanceof AppException) {
            AppException ae = (AppException) e;
            AppLogger.getLogger().log(Level.WARNING, "业务异常: code={0}, message={1}", new Object[]{ae.getCode(), ae.getMessage()});
            resp.setStatus(ae.getCode());

            if (isJson) {
                resp.setContentType("application/json;charset=UTF-8");
                String json = "{\"code\":" + ae.getCode() + ",\"message\":\"" + ae.getMessage() + "\",\"success\":false}";
                resp.getWriter().println(json);
            } else {
                resp.setContentType("text/html;charset=utf-8");
                resp.getWriter().write("<h3>操作失败</h3><p>" + ae.getMessage() + "</p>");
            }
        } else {
            AppLogger.getLogger().log(Level.SEVERE, "系统异常: ", e);
            resp.setStatus(500);

            if (isJson) {
                resp.setContentType("application/json;charset=UTF-8");
                String json = "{\"code\":500,\"message\":\"系统开小差了，请稍后重试\",\"success\":false}";
                resp.getWriter().println(json);
            } else {
                resp.setContentType("text/html;charset=utf-8");
                resp.getWriter().write("<h3>系统开小差了</h3><p>请联系赛博管理员检查日志</p>");
            }
        }
    }
}
