package springMVC;


import java.lang.annotation.*;

@Target(ElementType.PARAMETER) //方法参数
@Retention(RetentionPolicy.RUNTIME)
@Documented
//前端参数和java方法变量名的绑定
public @interface MyRequestParam {
    String value() default "";
}
