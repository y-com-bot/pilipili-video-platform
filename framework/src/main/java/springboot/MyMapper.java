package springboot;

import java.lang.annotation.*;

/**
 * 标记一个接口是 MyBatis 风格的 Mapper。
 *
 * 被标记后，框架启动时会为它创建代理对象，
 * 让上层可以像使用真实 MyBatis 一样直接注入接口。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyMapper {
    String value() default "";
}
