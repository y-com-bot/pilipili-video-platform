package framework.springboot;

import java.lang.annotation.*;

/**
 * 这个注解用来标记“启动类”。
 *
 * 真实 Spring Boot 里的 @SpringBootApplication 本质上是一个组合注解，
 * 里面包含了配置类、组件扫描、自动配置等能力。
 *
 * 这里我们做一个“面试项目版”的精简实现：
 * 1. 标记哪个类是应用入口
 * 2. 指定要扫描的基础包
 * 3. 指定内嵌 HTTP 服务端口
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MySpringBootApplication {

    /**
     * 指定要扫描的基础包。
     *
     * 如果不写，框架会默认从启动类所在包开始扫描。
     */
    String[] scanBasePackages() default {};

    /**
     * 内嵌 HTTP 服务端口。
     *
     * 为了简单，我们先不接 properties/yml，
     * 直接通过注解把最关键的启动参数放出来。
     */
    int port() default 8080;
}
