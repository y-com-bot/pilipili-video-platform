package springboot;

import java.lang.annotation.*;

/**
 * 最基础的组件注解。
 *
 * 只要一个类被它标记，就说明这个类可以交给 IoC 容器管理。
 * 后面的 Controller、Service、Configuration 都会建立在这个概念之上。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyComponent {
    String value() default "";
}
