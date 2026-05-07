package framework.springboot;

import java.lang.annotation.*;

/**
 * 绑定请求参数名。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value() default "";
}
