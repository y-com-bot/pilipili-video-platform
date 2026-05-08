package springboot;

import java.lang.annotation.*;

/**
 * 请求映射注解。
 *
 * 为了比你之前的 springMVC 版本更像一点“Boot + MVC”的组合，
 * 这里额外加了一个 method 属性，用来区分 GET / POST。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value() default "";

    String method() default "GET";
}
