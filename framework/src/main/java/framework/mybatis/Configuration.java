package framework.mybatis;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 这是手写 MyBatis 框架的全局配置对象。
 *
 * 它承担两类职责：
 * 1. 保存数据库连接信息
 * 2. 保存 statementId -> MappedStatement 的映射关系
 *
 * 真实 MyBatis 的 Configuration 会更复杂，
 * 里面还会维护缓存、插件、类型处理器、环境配置等大量能力。
 * 这里我们保留最核心、最适合面试项目讲解的部分。
 */
public class Configuration {

    /**
     * JDBC 驱动类名。
     *
     * 从 JDBC 4 开始，很多驱动会自动注册，
     * 所以这个字段允许为空；如果用户主动配置了驱动类名，
     * 工厂在创建连接前会显式加载它。
     */
    private String jdbcDriver;

    /**
     * JDBC 连接地址，例如：
     * jdbc:mysql://localhost:3306/video_platform
     */
    private String jdbcUrl;

    /**
     * 数据库用户名。
     */
    private String username;

    /**
     * 数据库密码。
     */
    private String password;

    /**
     * 保存所有 SQL 映射信息。
     *
     * key 一般是：
     * 全限定 Mapper 接口名 + "." + 方法名
     *
     * 例如：
     * com.yuan.StudentMapper.getById
     */
    private final Map<String, MappedStatement> mappedStatements = new LinkedHashMap<>();

    public Configuration() {
    }

    public Configuration(String jdbcDriver, String jdbcUrl, String username, String password) {
        this.jdbcDriver = jdbcDriver;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, MappedStatement> getMappedStatements() {
        return mappedStatements;
    }

    /**
     * 整体替换映射表时，先清空再拷贝，
     * 避免外部直接持有内部 Map 的引用。
     */
    public void setMappedStatements(Map<String, MappedStatement> mappedStatements) {
        this.mappedStatements.clear();
        if (mappedStatements != null) {
            this.mappedStatements.putAll(mappedStatements);
        }
    }

    /**
     * 注册一条 SQL 映射。
     */
    public void addMappedStatement(String statementId, MappedStatement mappedStatement) {
        if (statementId == null || statementId.trim().isEmpty()) {
            throw new IllegalArgumentException("statementId 不能为空");
        }
        if (mappedStatement == null) {
            throw new IllegalArgumentException("mappedStatement 不能为空");
        }
        this.mappedStatements.put(statementId.trim(), mappedStatement);
    }

    /**
     * 根据 statementId 获取映射信息。
     */
    public MappedStatement getMappedStatement(String statementId) {
        if (statementId == null) {
            return null;
        }
        return mappedStatements.get(statementId.trim());
    }

    /**
     * 启动前做一层基础校验，尽早暴露配置问题。
     */
    public void validate() {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalStateException("JDBC URL 不能为空");
        }
        if (username == null) {
            throw new IllegalStateException("数据库用户名不能为 null");
        }
        if (password == null) {
            throw new IllegalStateException("数据库密码不能为 null");
        }
    }
}
