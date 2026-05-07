package framework.springMVC;

@MyController
@MyRequestMapping("/api/user")
public class UserController {

    // 完整的请求路径将会是：/api/user/detail?id=xxx
    @MyRequestMapping("/detail")
    @MyResponseBody
    public String getUserDetail(@MyRequestParam("id") String userId) {
        // 这里暂时用假数据模拟
        System.out.println("接收到查询用户的请求，用户ID为: " + userId);
        return "{\"id\":\"" + userId + "\", \"name\":\"张三\", \"status\":\"success\"}";
    }
}
