package framework.springMVC;


import java.lang.annotation.*;

@Target(ElementType.METHOD) //方法
@Retention(RetentionPolicy.RUNTIME)
@Documented

//返回josn数据
public @interface MyResponseBody {
}
