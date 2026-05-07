package framework.springboot;

/**
 * 启动类示例。
 *
 * 运行这个 main 方法后，框架会完成：
 * 1. 扫描 framework.springboot 包
 * 2. 初始化配置类、Service、Controller
 * 3. 建立请求映射
 * 4. 启动内嵌 HTTP 服务
 *
 * 这样你在面试时既能展示框架代码，也能直接运行给面试官看。
 */
@MySpringBootApplication(scanBasePackages = "framework.springboot", port = 8080)
public class DemoApplication {

    public static void main(String[] args) {
        MySpringApplication.run(DemoApplication.class, args);
    }
}
