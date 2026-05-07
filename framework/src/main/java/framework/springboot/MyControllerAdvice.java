package framework.springboot;

import java.lang.annotation.*;

/**
 * 标记全局异常处理器。
 *
 * 被这个注解标记的类不会直接处理业务请求，
 * 而是专门用来统一接住 Controller 层抛出来的异常。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyControllerAdvice {
}
