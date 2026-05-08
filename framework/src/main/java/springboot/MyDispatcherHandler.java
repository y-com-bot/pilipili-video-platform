package springboot;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 这是请求分发器，作用类似于 Spring MVC 里的 DispatcherServlet。
 *
 * 区别在于：
 * 1. 这里不是跑在外部 Servlet 容器里
 * 2. 而是直接基于 JDK 自带 HttpServer 做最小实现
 * 3. 并且它会接入全局异常处理器，把控制器异常统一收口
 */
public class MyDispatcherHandler {

    private final MyApplicationContext applicationContext;

    public MyDispatcherHandler(MyApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            doDispatch(exchange);
        } catch (Throwable throwable) {
            handleException(exchange, throwable);
        } finally {
            exchange.close();
        }
    }

    private void doDispatch(HttpExchange exchange) throws Exception {
        String requestMethod = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String requestPath = normalizeUrl(exchange.getRequestURI().getPath());
        String mappingKey = requestMethod + ":" + requestPath;

        MyHandlerMethod handlerMethod = applicationContext.getHandlerMapping().get(mappingKey);
        if (handlerMethod == null) {
            writeResponse(exchange, 404, "text/plain;charset=UTF-8",
                    "404 Not Found: " + requestMethod + " " + requestPath);
            return;
        }

        Object[] args = buildMethodArgs(exchange, handlerMethod.getMethod());
        Object result;

        try {
            result = handlerMethod.getMethod().invoke(handlerMethod.getController(), args);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(target);
        }

        writeReturnValue(exchange, handlerMethod.getMethod(), result, 200);
    }

    private void handleException(HttpExchange exchange, Throwable throwable) throws IOException {
        Throwable target = unwrapThrowable(throwable);
        MyExceptionHandlerMethod exceptionHandlerMethod = applicationContext.resolveExceptionHandler(target);

        if (exceptionHandlerMethod != null) {
            try {
                Object[] args = buildExceptionHandlerArgs(exchange, target, exceptionHandlerMethod.getMethod());
                Object result = exceptionHandlerMethod.getMethod().invoke(exceptionHandlerMethod.getBean(), args);
                writeReturnValue(exchange, exceptionHandlerMethod.getMethod(), result, 500);
                return;
            } catch (Exception e) {
                target = unwrapThrowable(e);
            }
        }

        writeResponse(exchange, 500, "application/json;charset=UTF-8",
                "{\"code\":500,\"message\":\"系统异常: " + safeMessage(target) + "\"}");
    }

    private Object[] buildMethodArgs(HttpExchange exchange, Method method) throws IOException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameterTypes.length];
        Map<String, String> requestParams = extractRequestParams(exchange);

        for (int i = 0; i < parameters.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = parameters[i];

            if (parameterType == HttpExchange.class) {
                args[i] = exchange;
                continue;
            }

            if (parameter.isAnnotationPresent(MyRequestParam.class)) {
                String paramName = parameter.getAnnotation(MyRequestParam.class).value().trim();
                String value = requestParams.get(paramName);
                args[i] = convertType(value, parameterType);
            }
        }

        return args;
    }

    private Object[] buildExceptionHandlerArgs(HttpExchange exchange, Throwable throwable, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpExchange.class) {
                args[i] = exchange;
            } else if (Throwable.class.isAssignableFrom(parameterType)) {
                args[i] = throwable;
            }
        }

        return args;
    }

    private Map<String, String> extractRequestParams(HttpExchange exchange) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();

        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && !query.isEmpty()) {
            parseParamString(query, params);
        }

        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("POST".equals(method) || "PUT".equals(method)) {
            String body = readRequestBody(exchange.getRequestBody());
            if (!body.isEmpty()) {
                parseParamString(body, params);
            }
        }

        return params;
    }

    private void parseParamString(String source, Map<String, String> target) {
        String[] pairs = source.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }

            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            target.put(key, value);
        }
    }

    private String readRequestBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 返回值处理逻辑。
     *
     * 这里保持“简单但可讲”：
     * - @MyResponseBody：按 JSON 文本写回
     * - 其他情况：按纯文本写回
     */
    private void writeReturnValue(HttpExchange exchange, Method method, Object result, int statusCode) throws IOException {
        String body = result == null ? "" : String.valueOf(result);

        if (method.isAnnotationPresent(MyResponseBody.class)) {
            writeResponse(exchange, statusCode, "application/json;charset=UTF-8", body);
            return;
        }

        writeResponse(exchange, statusCode, "text/plain;charset=UTF-8", body);
    }

    @SuppressWarnings("unchecked")
    private Object convertType(String value, Class<?> type) {
        if (value == null || value.trim().isEmpty()) {
            if (type.isPrimitive()) {
                if (type == int.class) return 0;
                if (type == long.class) return 0L;
                if (type == double.class) return 0.0;
                if (type == float.class) return 0F;
                if (type == boolean.class) return false;
                if (type == short.class) return (short) 0;
                if (type == byte.class) return (byte) 0;
                if (type == char.class) return '\0';
            }
            return null;
        }

        String trimmed = value.trim();

        if (type == String.class) return trimmed;
        if (type == Integer.class || type == int.class) return Integer.parseInt(trimmed);
        if (type == Long.class || type == long.class) return Long.parseLong(trimmed);
        if (type == Double.class || type == double.class) return Double.parseDouble(trimmed);
        if (type == Float.class || type == float.class) return Float.parseFloat(trimmed);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(trimmed);
        if (type == Short.class || type == short.class) return Short.parseShort(trimmed);
        if (type == Byte.class || type == byte.class) return Byte.parseByte(trimmed);
        if (type == Character.class || type == char.class) return trimmed.charAt(0);
        if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(trimmed);
        if (type == java.time.LocalDate.class) return java.time.LocalDate.parse(trimmed);
        if (type == java.time.LocalDateTime.class) return java.time.LocalDateTime.parse(trimmed);
        if (type.isEnum()) return Enum.valueOf((Class<Enum>) type, trimmed);

        return trimmed;
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    private String normalizeUrl(String path) {
        String normalized = (path == null ? "" : path.trim()).replaceAll("/+", "/");
        if (normalized.isEmpty()) {
            return "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replaceAll("/+", "/");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private Throwable unwrapThrowable(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException
                && invocationTargetException.getTargetException() != null) {
            return invocationTargetException.getTargetException();
        }
        if (throwable instanceof RuntimeException runtimeException
                && runtimeException.getCause() != null) {
            return runtimeException.getCause();
        }
        return throwable;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null ? throwable.getClass().getSimpleName() : message.replace("\"", "'");
    }
}
