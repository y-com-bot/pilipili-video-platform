package framework.springboot;

import java.lang.annotation.*;

/**
 * 标记在配置类方法上，表示该方法的返回值也要注册到容器。
 *
 * 这是最简化版的“自动装配”入口之一。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyBean {
    String value() default "";
}
