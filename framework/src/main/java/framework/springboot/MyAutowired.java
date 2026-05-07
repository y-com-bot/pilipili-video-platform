package framework.springboot;

import java.lang.annotation.*;

/**
 * 字段注入注解。
 *
 * 默认按类型注入；
 * 如果 value 有值，则优先按名称注入。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}
