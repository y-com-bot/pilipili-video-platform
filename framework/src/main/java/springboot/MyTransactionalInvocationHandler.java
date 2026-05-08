package springboot;

import mybatis.SqlSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 事务代理处理器。
 *
 * 只要某个 Service 被 @MyTransactional 标记，
 * 并且它实现了接口，就会被这个代理包起来。
 */
public class MyTransactionalInvocationHandler implements InvocationHandler {

    private final Object target;
    private final MyTransactionManager transactionManager;

    public MyTransactionalInvocationHandler(Object target, MyTransactionManager transactionManager) {
        this.target = target;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        boolean transactional = target.getClass().isAnnotationPresent(MyTransactional.class)
                || targetMethod.isAnnotationPresent(MyTransactional.class);

        if (!transactional || transactionManager == null) {
            return invokeTarget(targetMethod, args);
        }

        // 如果当前线程已经存在事务，直接复用，避免重复开启事务。
        if (transactionManager.hasActiveSession()) {
            return invokeTarget(targetMethod, args);
        }

        SqlSession sqlSession = transactionManager.openNewSession();
        transactionManager.bind(sqlSession);

        try {
            Object result = invokeTarget(targetMethod, args);
            sqlSession.commit();
            return result;
        } catch (Throwable throwable) {
            sqlSession.rollback();
            throw throwable;
        } finally {
            transactionManager.clear();
            sqlSession.close();
        }
    }

    private Object invokeTarget(Method targetMethod, Object[] args) throws Throwable {
        try {
            return targetMethod.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
