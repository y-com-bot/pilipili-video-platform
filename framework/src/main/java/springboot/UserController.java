package springboot;

import com.sun.net.httpserver.HttpExchange;

/**
 * 示例控制器。
 *
 * 它主要用来演示三件事：
 * 1. Controller 会被自动扫描并注册
 * 2. UserService 会被自动注入
 * 3. 请求参数能够绑定到方法参数上
 */
@MyController
@MyRequestMapping("/boot/user")
public class UserController {

    @MyAutowired
    private UserService userService;

    /**
     * 最核心的演示接口。
     *
     * 访问方式：
     * http://localhost:8080/boot/user/detail?id=1001
     */
    @MyRequestMapping(value = "/detail", method = "GET")
    @MyResponseBody
    public String getUserDetail(@MyRequestParam("id") Long userId) {
        return userService.getUserDetail(userId);
    }

    /**
     * 额外补一个健康检查接口，方便快速验证服务是否启动成功。
     */
    @MyRequestMapping(value = "/ping", method = "GET")
    @MyResponseBody
    public String ping(HttpExchange exchange) {
        return "{\"code\":200,\"message\":\"pong\",\"path\":\"" + exchange.getRequestURI().getPath() + "\"}";
    }
}
