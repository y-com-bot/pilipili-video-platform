package framework.springboot;

import java.lang.reflect.Method;

/**
 * 这个类是一次请求映射关系的载体。
 *
 * 为什么要单独抽一个对象出来，而不是只存 Method？
 * 因为请求分发时，除了 Method，我们还需要知道：
 * 1. 这个方法属于哪个 Controller 实例
 * 2. 支持什么 HTTP 方法
 * 3. 映射的 URL 是什么
 *
 * 把这些信息放在一起，后面调度会更清晰。
 */
public class MyHandlerMethod {
    private final String httpMethod;
    private final String url;
    private final Object controller;
    private final Method method;

    public MyHandlerMethod(String httpMethod, String url, Object controller, Method method) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.controller = controller;
        this.method = method;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }
}
