package com.yuan.utils;

/**
 * 前后端之间现在主要用字符串形式的 JSON 交互，
 * 这个工具类专门负责最常用的 JSON 文本拼装和转义，
 * 避免每个控制器里都重复写一堆 replace。
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    public static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    public static String success(String message) {
        return "{\"code\":200,\"success\":true,\"message\":" + toJsonString(message) + "}";
    }

    public static String successWithData(String message, String dataJson) {
        return "{\"code\":200,\"success\":true,\"message\":" + toJsonString(message)
                + ",\"data\":" + (dataJson == null ? "null" : dataJson) + "}";
    }

    public static String error(int code, String message) {
        return "{\"code\":" + code + ",\"success\":false,\"message\":" + toJsonString(message) + "}";
    }
}
