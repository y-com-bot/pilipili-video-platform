package springboot;

import java.lang.annotation.*;

/**
 * 指定要扫描哪些包下的 Mapper 接口。
 *
 * 这个注解更接近真实 Spring Boot + MyBatis 的使用体验，
 * 适合把“数据访问层自动注册”这条主线讲清楚。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyMapperScan {
    String[] value() default {};
}
