package springboot;

import mybatis.SqlSession;
import mybatis.SqlSessionFactory;

/**
 * 这是一个极简版事务管理器。
 *
 * 它的核心思路是：
 * 1. 通过 ThreadLocal 把当前线程正在使用的 SqlSession 绑定起来
 * 2. @MyTransactional 方法进入时打开事务
 * 3. 方法成功则提交，失败则回滚
 * 4. Mapper 代理如果发现当前线程已经有会话，就直接复用
 *
 * 这样就把“事务管理”和“数据访问整合”真正串起来了。
 */
public class MyTransactionManager {

    private final SqlSessionFactory sqlSessionFactory;
    private final ThreadLocal<SqlSession> currentSession = new ThreadLocal<>();

    public MyTransactionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public boolean hasActiveSession() {
        return currentSession.get() != null;
    }

    public SqlSession getCurrentSession() {
        return currentSession.get();
    }

    public SqlSession openNewSession() throws Exception {
        return sqlSessionFactory.openSession();
    }

    public void bind(SqlSession sqlSession) {
        currentSession.set(sqlSession);
    }

    public void clear() {
        currentSession.remove();
    }
}
