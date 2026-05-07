package framework.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基于 JDBC Connection 的事务实现。
 *
 * 这一层的核心思想很简单：
 * 框架不自己发明事务机制，而是把 JDBC 本身的事务能力包一层，
 * 给上层 SqlSession 提供统一接口。
 */
public class JdbcTransaction implements Transaction {

    private final Connection connection;

    public JdbcTransaction(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection 不能为空");
        }
        this.connection = connection;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection.isClosed()) {
            throw new SQLException("数据库连接已经关闭");
        }
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }
        return connection;
    }

    @Override
    public void commit() throws SQLException {
        if (!connection.isClosed() && !connection.getAutoCommit()) {
            connection.commit();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (!connection.isClosed() && !connection.getAutoCommit()) {
            connection.rollback();
        }
    }

    @Override
    public void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
    }
}
