package mybatis;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 事务抽象层。
 *
 * 这一层把底层事务能力统一封装起来，
 * 让上层只需要关心“提交 / 回滚 / 关闭”，
 * 不需要关心具体是 JDBC 事务还是别的实现。
 */
public interface Transaction {

    /**
     * 获取当前事务持有的数据库连接。
     */
    Connection getConnection() throws SQLException;

    /**
     * 提交事务。
     */
    void commit() throws SQLException;

    /**
     * 回滚事务。
     */
    void rollback() throws SQLException;

    /**
     * 关闭事务及相关资源。
     */
    void close() throws SQLException;
}
