package springMVC;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface MyHandlerInterceptor {
    /**
     * 在业务 Controller 执行之前被调用。
     * @return true 放行，执行下一个拦截器或目标方法；false 拦截，直接返回。
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
