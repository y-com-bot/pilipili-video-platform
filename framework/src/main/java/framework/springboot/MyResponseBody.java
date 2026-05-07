package framework.springboot;

import java.lang.annotation.*;

/**
 * 标记方法返回值直接写回响应体。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyResponseBody {
}
