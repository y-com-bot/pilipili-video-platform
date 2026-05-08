package mybatis;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 默认的 SqlSessionFactory 实现。
 *
 * 它的作用就是：
 * 根据 Configuration 创建数据库连接，
 * 再把连接、事务、执行器包装成一个完整的 SqlSession。
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration 不能为空");
        }
        this.configuration = configuration;
    }

    @Override
    public SqlSession openSession() throws Exception {
        configuration.validate();
        loadDriverIfNecessary();

        Connection connection = DriverManager.getConnection(
                configuration.getJdbcUrl(),
                configuration.getUsername(),
                configuration.getPassword()
        );

        Transaction transaction = new JdbcTransaction(connection);
        Executor executor = new SimpleExecutor(transaction);
        return new DefaultSqlSession(configuration, executor, transaction);
    }

    /**
     * 如果用户显式配置了驱动类名，则主动加载。
     *
     * 这样做有两个好处：
     * 1. 对老版本驱动更兼容
     * 2. 配置错了时能更早抛错
     */
    private void loadDriverIfNecessary() throws ClassNotFoundException {
        String driver = configuration.getJdbcDriver();
        if (driver != null && !driver.trim().isEmpty()) {
            Class.forName(driver.trim());
        }
    }
}
