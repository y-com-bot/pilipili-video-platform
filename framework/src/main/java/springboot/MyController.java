package springboot;

import java.lang.annotation.*;

/**
 * 标记控制层组件。
 *
 * 这里不做复杂的元注解解析，框架会直接判断这个注解是否存在。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}
