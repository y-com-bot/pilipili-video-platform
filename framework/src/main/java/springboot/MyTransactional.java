package springboot;

import java.lang.annotation.*;

/**
 * 标记一个类或方法需要事务支持。
 *
 * 这里实现的是“面试项目版”的简化事务：
 * 1. 基于动态代理
 * 2. 依赖 MyBatis 的 SqlSession 事务能力
 * 3. 只处理最常见的提交 / 回滚场景
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyTransactional {
}
