package springboot;

import java.lang.annotation.*;

/**
 * 标记异常处理方法。
 *
 * 例如：
 * @MyExceptionHandler(RuntimeException.class)
 * public String handle(RuntimeException e) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyExceptionHandler {
    Class<? extends Throwable>[] value() default {};
}
