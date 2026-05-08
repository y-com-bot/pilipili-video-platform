package springMVC;

import java.lang.annotation.*;

@Target(ElementType.TYPE) // 作用在类上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    // 允许自定义 Bean 的名称，默认空
    String value() default "";
}