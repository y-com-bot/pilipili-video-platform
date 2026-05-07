package framework.mybatis;

/**
 * SqlSessionFactory 用来创建 SqlSession。
 *
 * 它的存在意义在于：
 * 把“如何创建一个带连接、带事务、带执行器的完整会话”
 * 这一件事集中到一起，业务层只需要调用 openSession 即可。
 */
public interface SqlSessionFactory {
    SqlSession openSession() throws Exception;
}
