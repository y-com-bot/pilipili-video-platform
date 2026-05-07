package framework.springboot;

import java.lang.annotation.*;

/**
 * 标记业务层组件。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    String value() default "";
}
