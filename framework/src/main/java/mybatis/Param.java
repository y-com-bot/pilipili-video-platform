package mybatis;

import java.lang.annotation.*;

/**
 * 给 Mapper 方法参数显式命名。
 *
 * 例如：
 * User selectByIdAndStatus(@Param("id") Integer id, @Param("status") Integer status)
 *
 * 对应 SQL 可以写成：
 * select * from user where id = #{id} and status = #{status}
 *
 * 如果没有这个注解，多参数场景下框架只能退化成 arg0、arg1、param1 这类名称。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
    String value();
}
