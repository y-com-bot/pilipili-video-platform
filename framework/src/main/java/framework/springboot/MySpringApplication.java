package framework.springboot;

/**
 * 模拟 SpringApplication.run(...) 的启动入口。
 *
 * 真实 Spring Boot 的启动过程非常长，
 * 但面试项目里最重要的是把主线讲明白：
 * 1. 读取启动类配置
 * 2. 创建应用上下文
 * 3. 扫描并初始化 Bean
 * 4. 创建请求映射
 * 5. 启动内嵌服务
 */
public class MySpringApplication {

    public static MyApplicationContext run(Class<?> applicationClass, String[] args) {
        MySpringBootApplication application = applicationClass.getAnnotation(MySpringBootApplication.class);
        if (application == null) {
            throw new RuntimeException("启动类必须标注 @MySpringBootApplication");
        }

        MyApplicationContext applicationContext = new MyApplicationContext(applicationClass);
        applicationContext.refresh();

        MyDispatcherHandler dispatcherHandler = new MyDispatcherHandler(applicationContext);
        MyEmbeddedServer embeddedServer = new MyEmbeddedServer(application.port(), dispatcherHandler);
        embeddedServer.start();

        printStartupLog(applicationClass, application.port(), applicationContext);
        return applicationContext;
    }

    private static void printStartupLog(Class<?> applicationClass, int port, MyApplicationContext applicationContext) {
        System.out.println("=================================================");
        System.out.println("Mini SpringBoot 启动成功");
        System.out.println("启动类: " + applicationClass.getName());
        System.out.println("端口: " + port);
        System.out.println("可访问示例: http://localhost:" + port + "/boot/user/detail?id=1001");
        System.out.println("已注册请求映射数量: " + applicationContext.getHandlerMapping().size());
        System.out.println("=================================================");
    }
}
