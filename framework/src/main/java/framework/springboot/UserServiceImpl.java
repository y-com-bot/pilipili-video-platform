package framework.springboot;

/**
 * 业务实现类。
 */
@MyService
public class UserServiceImpl implements UserService {

    @MyAutowired
    private AppInfo appInfo;

    @Override
    public String getUserDetail(Long userId) {
        return "{\"id\":" + userId
                + ",\"name\":\"张三\""
                + ",\"source\":\"" + appInfo.getAppName() + "\""
                + ",\"version\":\"" + appInfo.getVersion() + "\""
                + ",\"status\":\"success\"}";
    }
}
