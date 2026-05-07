package framework.springMVC;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 这是 servlet 版本的迷你 DispatcherServlet。
 *
 * 相比前一版，这一版补齐了几条很重要的链路：
 * 1. 支持全局拦截器
 * 2. 支持全局异常处理器
 * 3. 对 Bean 注册、路由映射、依赖注入做了更多校验
 *
 * 这样它就不再只是“能把 URL 调到方法上”，
 * 而是更接近一个完整的 MVC 请求分发核心。
 */
@WebServlet(name = "myDispatcherServlet", urlPatterns = "/*", loadOnStartup = 1)
public class MyDispatcherServlet extends HttpServlet {

    private final List<String> classNames = new ArrayList<>();
    private final Map<String, Object> iocContainer = new LinkedHashMap<>();
    private final Map<String, Method> handlerMapping = new LinkedHashMap<>();
    private final Map<String, Object> urlControllerMap = new LinkedHashMap<>();
    private final List<MyHandlerInterceptor> interceptors = new ArrayList<>();
    private final Map<Class<? extends Throwable>, ExceptionAdviceMethod> exceptionHandlerMappings = new LinkedHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        doScan("com.yuan");
        doInstance();
        doAutowired();
        initInterceptors();
        initExceptionHandlerMappings();
        initHandlerMapping();
        System.out.println("========== MiniSpringMVC 初始化完成 ==========");
    }

    private void doScan(String basePackage) {
        URL url = getClass().getClassLoader().getResource(basePackage.replace('.', '/'));
        if (url == null) {
            return;
        }

        File dir = new File(url.getFile());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                doScan(basePackage + "." + file.getName());
                continue;
            }

            if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().replace(".class", "");
                if (!classNames.contains(className)) {
                    classNames.add(className);
                }
            }
        }
    }

    /**
     * 实例化容器中要管理的对象。
     *
     * 当前支持：
     * 1. @MyController
     * 2. @MyService
     * 3. @MyControllerAdvice
     * 4. 实现了 MyHandlerInterceptor 的普通类
     */
    private void doInstance() {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                if (clazz.isAnnotationPresent(MyController.class)
                        || clazz.isAnnotationPresent(MyService.class)
                        || clazz.isAnnotationPresent(MyControllerAdvice.class)
                        || MyHandlerInterceptor.class.isAssignableFrom(clazz)) {

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    registerBean(resolveBeanName(clazz), instance);

                    // Service 的接口名也注册进去，方便按接口注入
                    if (clazz.isAnnotationPresent(MyService.class)) {
                        for (Class<?> interfaceType : clazz.getInterfaces()) {
                            registerAlias(interfaceType.getName(), instance);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("实例化 Bean 失败: " + className, e);
            }
        }
    }

    private void registerBean(String beanName, Object instance) {
        if (iocContainer.containsKey(beanName) && iocContainer.get(beanName) != instance) {
            throw new RuntimeException("重复的 Bean 名称: " + beanName);
        }
        iocContainer.put(beanName, instance);
    }

    private void registerAlias(String alias, Object instance) {
        if (iocContainer.containsKey(alias) && iocContainer.get(alias) != instance) {
            throw new RuntimeException("重复的 Bean 类型别名: " + alias);
        }
        iocContainer.put(alias, instance);
    }

    private void doAutowired() {
        for (Object bean : new LinkedHashSet<>(iocContainer.values())) {
            java.lang.reflect.Field[] fields = bean.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                Object dependency = resolveDependency(field, autowired);
                if (dependency == null) {
                    throw new RuntimeException("依赖注入失败，找不到 Bean: " + field.getType().getName());
                }

                try {
                    field.setAccessible(true);
                    field.set(bean, dependency);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("字段注入失败: " + field.getName(), e);
                }
            }
        }
    }

    private Object resolveDependency(java.lang.reflect.Field field, MyAutowired autowired) {
        String beanName = autowired.value().trim();
        if (!beanName.isEmpty()) {
            return iocContainer.get(beanName);
        }

        Object dependency = iocContainer.get(field.getType().getName());
        if (dependency != null) {
            return dependency;
        }

        return iocContainer.get(field.getName());
    }

    private void initInterceptors() {
        LinkedHashSet<Object> uniqueBeans = new LinkedHashSet<>(iocContainer.values());
        for (Object bean : uniqueBeans) {
            if (bean instanceof MyHandlerInterceptor interceptor) {
                interceptors.add(interceptor);
            }
        }
    }

    private void initExceptionHandlerMappings() {
        LinkedHashSet<Object> uniqueBeans = new LinkedHashSet<>(iocContainer.values());
        for (Object bean : uniqueBeans) {
            Class<?> clazz = bean.getClass();
            if (!clazz.isAnnotationPresent(MyControllerAdvice.class)) {
                continue;
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(MyExceptionHandler.class)) {
                    continue;
                }

                for (Class<? extends Throwable> exceptionType : resolveExceptionTypes(method)) {
                    if (exceptionHandlerMappings.containsKey(exceptionType)) {
                        throw new RuntimeException("重复的全局异常处理器: " + exceptionType.getName());
                    }
                    method.setAccessible(true);
                    exceptionHandlerMappings.put(exceptionType, new ExceptionAdviceMethod(bean, method));
                }
            }
        }
    }

    private void initHandlerMapping() {
        LinkedHashSet<Object> uniqueBeans = new LinkedHashSet<>(iocContainer.values());
        for (Object bean : uniqueBeans) {
            Class<?> clazz = bean.getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                baseUrl = clazz.getAnnotation(MyRequestMapping.class).value();
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                String url = normalizeUrl(baseUrl + "/" + method.getAnnotation(MyRequestMapping.class).value());
                if (handlerMapping.containsKey(url)) {
                    throw new RuntimeException("重复的请求映射: " + url);
                }

                handlerMapping.put(url, method);
                urlControllerMap.put(url, bean);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = normalizeUrl(req.getRequestURI().replace(req.getContextPath(), ""));
        Method method = handlerMapping.get(url);
        Object controller = urlControllerMap.get(url);

        if (method == null || controller == null) {
            if (tryForwardStaticResource(url, req, resp)) {
                return;
            }
            resp.setStatus(404);
            resp.setContentType("text/html;charset=utf-8");
            resp.getWriter().write("<h1>404 Not Found</h1> 找不到对应的接口: " + url);
            return;
        }

        for (MyHandlerInterceptor interceptor : interceptors) {
            if (!interceptor.preHandle(req, resp)) {
                return;
            }
        }

        try {
            Object[] args = buildMethodArgs(req, resp, method);
            Object result = method.invoke(controller, args);
            writeReturnValue(req, resp, method, result, 200);
        } catch (InvocationTargetException e) {
            throw (e.getTargetException() instanceof Exception ex) ? ex : new RuntimeException(e.getTargetException());
        }
    }

    private Object[] buildMethodArgs(HttpServletRequest req, HttpServletResponse resp, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameterTypes.length];
        Map<String, String[]> reqParameterMap = req.getParameterMap();

        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameterTypes[i];
            java.lang.reflect.Parameter parameter = parameters[i];

            if (type == HttpServletRequest.class) {
                args[i] = req;
                continue;
            }
            if (type == HttpServletResponse.class) {
                args[i] = resp;
                continue;
            }

            if (parameter.isAnnotationPresent(MyRequestParam.class)) {
                String paramName = parameter.getAnnotation(MyRequestParam.class).value();
                if (reqParameterMap.containsKey(paramName) && reqParameterMap.get(paramName).length > 0) {
                    args[i] = convertType(reqParameterMap.get(paramName)[0], type);
                }
            }
        }

        return args;
    }

    private void handleException(HttpServletRequest req, HttpServletResponse resp, Throwable throwable) throws ServletException {
        Throwable target = unwrapThrowable(throwable);
        ExceptionAdviceMethod adviceMethod = resolveExceptionHandler(target);

        try {
            if (adviceMethod != null) {
                Object[] args = buildExceptionHandlerArgs(req, resp, target, adviceMethod.method);
                Object result = adviceMethod.method.invoke(adviceMethod.bean, args);
                writeReturnValue(req, resp, adviceMethod.method, result, 500);
                return;
            }

            resp.setStatus(500);
            resp.setContentType("application/json;charset=utf-8");
            try (PrintWriter writer = resp.getWriter()) {
                writer.write("{\"code\":500,\"status\":\"error\",\"message\":\"系统异常: " + safeMessage(target) + "\"}");
            }
        } catch (Exception e) {
            throw new ServletException("处理异常失败", e);
        }
    }

    private ExceptionAdviceMethod resolveExceptionHandler(Throwable throwable) {
        Class<?> current = throwable.getClass();
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            ExceptionAdviceMethod adviceMethod = exceptionHandlerMappings.get(current);
            if (adviceMethod != null) {
                return adviceMethod;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object[] buildExceptionHandlerArgs(HttpServletRequest req,
                                               HttpServletResponse resp,
                                               Throwable throwable,
                                               Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                args[i] = req;
            } else if (parameterType == HttpServletResponse.class) {
                args[i] = resp;
            } else if (Throwable.class.isAssignableFrom(parameterType)) {
                args[i] = throwable;
            }
        }

        return args;
    }

    private void writeReturnValue(HttpServletRequest req,
                                  HttpServletResponse resp,
                                  Method method,
                                  Object result,
                                  int statusCode) throws Exception {
        resp.setStatus(statusCode);

        if (method.isAnnotationPresent(MyResponseBody.class)) {
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write(result == null ? "" : String.valueOf(result));
            return;
        }

        if (result instanceof String viewName) {
            req.getRequestDispatcher("/" + viewName + ".jsp").forward(req, resp);
            return;
        }

        resp.setContentType("text/plain;charset=utf-8");
        resp.getWriter().write(result == null ? "" : String.valueOf(result));
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] resolveExceptionTypes(Method method) {
        MyExceptionHandler exceptionHandler = method.getAnnotation(MyExceptionHandler.class);
        if (exceptionHandler.value().length > 0) {
            return exceptionHandler.value();
        }

        List<Class<? extends Throwable>> types = new ArrayList<>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (Throwable.class.isAssignableFrom(parameterType)) {
                types.add((Class<? extends Throwable>) parameterType);
            }
        }
        return types.toArray(new Class[0]);
    }

    private String resolveBeanName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(MyController.class)) {
            String value = clazz.getAnnotation(MyController.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (clazz.isAnnotationPresent(MyService.class)) {
            String value = clazz.getAnnotation(MyService.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return toLowerFirstCase(clazz.getSimpleName());
    }

    private String normalizeUrl(String url) {
        String normalized = (url == null ? "" : url.trim()).replaceAll("/+", "/");
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

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
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

    private Throwable unwrapThrowable(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException
                && invocationTargetException.getTargetException() != null) {
            return invocationTargetException.getTargetException();
        }
        return throwable;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null ? throwable.getClass().getSimpleName() : message.replace("\"", "'");
    }

    /**
     * 当当前请求不是框架 Controller 路由时，
     * 尝试把它交还给 Web 容器去处理静态资源。
     *
     * 这一步非常重要，因为 DispatcherServlet 映射的是 /*，
     * 如果不主动放行，html/css/js/mp4 这类资源也会被它吃掉。
     */
    private boolean tryForwardStaticResource(String url,
                                             HttpServletRequest req,
                                             HttpServletResponse resp) throws Exception {
        // 处理根路径，加载欢迎文件
        if ("/".equals(url)) {
            url = "/index.html";
        }

        if (req.getServletContext().getResource(url) == null) {
            return false;
        }

        String mimeType = req.getServletContext().getMimeType(url);
        if (mimeType != null && !mimeType.isEmpty()) {
            resp.setContentType(mimeType);
        }

        try (InputStream inputStream = req.getServletContext().getResourceAsStream(url);
             OutputStream outputStream = resp.getOutputStream()) {
            if (inputStream == null) {
                return false;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            return true;
        }
    }

    private static class ExceptionAdviceMethod {
        private final Object bean;
        private final Method method;

        private ExceptionAdviceMethod(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }
    }
}
