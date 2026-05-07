package framework.springboot;

/**
 * 业务接口。
 *
 * 这里特地保留接口 + 实现类的写法，
 * 这样你面试时可以讲“我的 IoC 支持按接口注入”。
 */
public interface UserService {
    String getUserDetail(Long userId);
}
