package springboot;

/**
 * 一个普通 Bean，用来演示：
 * - 它不是 Controller
 * - 它不是 Service
 * - 它可以通过 @MyConfiguration + @MyBean 的方式自动注册到容器
 *
 * 这样就能把“SpringBoot 风格的配置注册”也展示出来。
 */
public class AppInfo {
    private final String appName;
    private final String version;

    public AppInfo(String appName, String version) {
        this.appName = appName;
        this.version = version;
    }

    public String getAppName() {
        return appName;
    }

    public String getVersion() {
        return version;
    }
}
