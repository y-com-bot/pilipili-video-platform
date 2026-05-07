package framework.springMVC;


import java.lang.annotation.*;

@Target(ElementType.TYPE) //用在类和接口上
@Retention(RetentionPolicy.RUNTIME) //运行时有效，让反射能够读取
@Documented
//控制层注解
public @interface MyController {
    String value() default "";
}
