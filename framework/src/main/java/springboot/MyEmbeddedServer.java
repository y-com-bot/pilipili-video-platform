package springboot;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 这是简化版“内嵌 Web 服务器”。
 *
 * 真实 Spring Boot 默认会帮我们启动内嵌 Tomcat/Jetty/Undertow。
 * 这里为了不引入额外依赖，直接用 JDK 自带的 HttpServer 来模拟。
 *
 * 这样做的好处是：
 * 1. 不需要额外 Maven 依赖
 * 2. 启动原理足够清晰
 * 3. 能非常直观地体现“Boot 启动后直接提供 HTTP 服务”
 */
public class MyEmbeddedServer {

    private final int port;
    private final MyDispatcherHandler dispatcherHandler;
    private HttpServer httpServer;

    public MyEmbeddedServer(int port, MyDispatcherHandler dispatcherHandler) {
        this.port = port;
        this.dispatcherHandler = dispatcherHandler;
    }

    /**
     * 启动内嵌服务。
     */
    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // 所有请求统一交给我们的分发器处理
            httpServer.createContext("/", exchange -> dispatcherHandler.handle(exchange));

            // 使用线程池，提高并发请求处理能力
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException("启动内嵌 HTTP 服务失败，端口: " + port, e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }
}
