package framework.springMVC;


import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD}) //类和方法
@Retention(RetentionPolicy.RUNTIME)
@Documented

//url映射注解
public @interface MyRequestMapping {
    String value() default "";  //url路径
}
