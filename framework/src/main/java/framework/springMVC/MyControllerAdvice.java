package framework.springMVC;

import java.lang.annotation.*;

/**
 * 标记全局异常处理类。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyControllerAdvice {
}
