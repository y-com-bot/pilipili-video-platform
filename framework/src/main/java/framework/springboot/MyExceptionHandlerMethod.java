package framework.springboot;

import java.lang.reflect.Method;

/**
 * 保存一条“异常类型 -> 处理方法”的映射关系。
 */
public class MyExceptionHandlerMethod {
    private final Class<? extends Throwable> exceptionType;
    private final Object bean;
    private final Method method;

    public MyExceptionHandlerMethod(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        this.exceptionType = exceptionType;
        this.bean = bean;
        this.method = method;
    }

    public Class<? extends Throwable> getExceptionType() {
        return exceptionType;
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }
}
