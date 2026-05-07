package framework.springMVC;

import java.lang.annotation.*;

@Target(ElementType.FIELD) // 重点：作用在类的属性字段上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    // 允许按名称注入，默认空则按类型注入
    String value() default "";
}
