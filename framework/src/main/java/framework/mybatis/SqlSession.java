package framework.mybatis;

import java.util.List;

/**
 * SqlSession 是开发者直接打交道的核心门面。
 *
 * 它封装了三类能力：
 * 1. 执行查询
 * 2. 执行增删改
 * 3. 获取 Mapper 接口代理对象
 *
 * 同时它也管理当前会话对应的事务生命周期。
 */
public interface SqlSession {

    <T> T selectOne(String statementId, Object parameter) throws Exception;

    <T> List<T> selectList(String statementId, Object parameter) throws Exception;

    int insert(String statementId, Object parameter) throws Exception;

    int update(String statementId, Object parameter) throws Exception;

    int delete(String statementId, Object parameter) throws Exception;

    void commit() throws Exception;

    void rollback() throws Exception;

    void close() throws Exception;

    <T> T getMapper(Class<T> type);
}
