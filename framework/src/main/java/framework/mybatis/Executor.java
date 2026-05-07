package framework.mybatis;

import java.util.List;

/**
 * Executor 是真正负责“执行 SQL”的抽象层。
 *
 * 这样设计的好处是：
 * 1. SqlSession 不需要直接处理 JDBC 细节
 * 2. 未来如果要扩展缓存执行器、批量执行器，会更容易
 */
public interface Executor {

    /**
     * 执行查询语句，并把结果映射成对象集合。
     */
    <T> List<T> query(MappedStatement ms, Object parameter) throws Exception;

    /**
     * 执行增删改语句，并返回影响行数。
     */
    int update(MappedStatement ms, Object parameter) throws Exception;
}
