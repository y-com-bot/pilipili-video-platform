package springboot;

import mybatis.SqlSession;
import mybatis.SqlSessionFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 这是 SpringBoot 容器里对 MyBatis Mapper 的整合代理。
 *
 * 它和 mybatis 包里的 MapperProxy 不是一层东西：
 * - mybatis.MapperProxy 负责“Mapper 方法 -> SQL”
 * - 这里这个代理负责“容器 Bean -> SqlSession 生命周期”
 *
 * 这样职责会更清楚。
 */
public class MyManagedMapperProxy implements InvocationHandler {

    private final Class<?> mapperInterface;
    private final SqlSessionFactory sqlSessionFactory;
    private final MyTransactionManager transactionManager;

    public MyManagedMapperProxy(Class<?> mapperInterface,
                                SqlSessionFactory sqlSessionFactory,
                                MyTransactionManager transactionManager) {
        this.mapperInterface = mapperInterface;
        this.sqlSessionFactory = sqlSessionFactory;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 如果当前线程已有事务会话，直接复用。
        if (transactionManager != null && transactionManager.hasActiveSession()) {
            SqlSession currentSession = transactionManager.getCurrentSession();
            Object mapper = currentSession.getMapper(mapperInterface);
            return method.invoke(mapper, args);
        }

        // 没有事务上下文时，按“方法级短会话”执行，做完就提交 / 关闭。
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            Object mapper = sqlSession.getMapper(mapperInterface);
            Object result = method.invoke(mapper, args);
            sqlSession.commit();
            return result;
        } catch (Throwable throwable) {
            sqlSession.rollback();
            if (throwable instanceof java.lang.reflect.InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getTargetException();
            }
            throw throwable;
        } finally {
            sqlSession.close();
        }
    }
}
