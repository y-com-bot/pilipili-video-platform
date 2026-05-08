package mybatis;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 默认的 SqlSession 实现。
 *
 * 这个类的职责非常明确：
 * 1. 根据 statementId 找到 MappedStatement
 * 2. 调用 Executor 执行 SQL
 * 3. 管理事务提交、回滚和关闭
 * 4. 为 Mapper 接口创建代理对象
 */
public class DefaultSqlSession implements SqlSession {

    private final Configuration configuration;
    private final Executor executor;
    private final Transaction transaction;
    private boolean closed;

    public DefaultSqlSession(Configuration configuration, Executor executor, Transaction transaction) {
        this.configuration = configuration;
        this.executor = executor;
        this.transaction = transaction;
    }

    @Override
    public <T> T selectOne(String statementId, Object parameter) throws Exception {
        List<T> list = selectList(statementId, parameter);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new RuntimeException("selectOne 查询结果超过一条，请检查 SQL 或改用 selectList: " + statementId);
        }
        return list.get(0);
    }

    @Override
    public <T> List<T> selectList(String statementId, Object parameter) throws Exception {
        ensureOpen();
        MappedStatement mappedStatement = getRequiredMappedStatement(statementId);
        return executor.query(mappedStatement, parameter);
    }

    @Override
    public int insert(String statementId, Object parameter) throws Exception {
        return executeUpdate(statementId, parameter, SqlCommandType.INSERT);
    }

    @Override
    public int update(String statementId, Object parameter) throws Exception {
        return executeUpdate(statementId, parameter, SqlCommandType.UPDATE);
    }

    @Override
    public int delete(String statementId, Object parameter) throws Exception {
        return executeUpdate(statementId, parameter, SqlCommandType.DELETE);
    }

    @Override
    public void commit() throws Exception {
        ensureOpen();
        transaction.commit();
    }

    @Override
    public void rollback() throws Exception {
        if (closed) {
            return;
        }
        transaction.rollback();
    }

    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }
        closed = true;
        transaction.close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> type) {
        ensureMapperInterface(type);
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new MapperProxy(this, type, configuration)
        );
    }

    private int executeUpdate(String statementId, Object parameter, SqlCommandType expectedType) throws Exception {
        ensureOpen();
        MappedStatement mappedStatement = getRequiredMappedStatement(statementId);

        if (mappedStatement.getSqlCommandType() != SqlCommandType.UNKNOWN
                && mappedStatement.getSqlCommandType() != expectedType) {
            throw new RuntimeException("SQL 类型与调用方法不匹配: " + statementId);
        }

        return executor.update(mappedStatement, parameter);
    }

    private MappedStatement getRequiredMappedStatement(String statementId) {
        MappedStatement mappedStatement = configuration.getMappedStatement(statementId);
        if (mappedStatement == null) {
            throw new RuntimeException("找不到对应的 SQL 映射: " + statementId);
        }
        if (mappedStatement.getSql() == null || mappedStatement.getSql().trim().isEmpty()) {
            throw new RuntimeException("SQL 不能为空: " + statementId);
        }
        return mappedStatement;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("SqlSession 已关闭，不能继续使用");
        }
    }

    private <T> void ensureMapperInterface(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Mapper 类型不能为空");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Mapper 必须是接口类型: " + type.getName());
        }
    }
}
