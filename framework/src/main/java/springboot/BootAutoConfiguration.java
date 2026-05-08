package springboot;

/**
 * 这是一个演示性质的“自动配置类”。
 *
 * 虽然它没有像真正 Spring Boot 那样根据条件装配，
 * 但已经体现出一个很关键的思路：
 * “某些 Bean 不需要开发者手动 new，而是由框架启动时自动创建并放入容器。”
 */
@MyConfiguration
public class BootAutoConfiguration {

    @MyBean
    public AppInfo appInfo() {
        return new AppInfo("mini-spring-boot", "1.0-demo");
    }
}
