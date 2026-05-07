package framework.springboot;

import java.lang.annotation.*;

/**
 * 标记配置类。
 *
 * 真实 Spring Boot 会把很多自动配置类都当成配置源来处理。
 * 我们这里只保留最关键的思想：
 * - 配置类本身也是一个 Bean
 * - 配置类中可以通过 @MyBean 声明额外 Bean
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyConfiguration {
    String value() default "";
}
