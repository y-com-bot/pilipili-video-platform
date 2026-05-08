package springMVC;

import java.lang.annotation.*;

/**
 * 标记异常处理方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyExceptionHandler {
    Class<? extends Throwable>[] value() default {};
}
