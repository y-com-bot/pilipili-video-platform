package framework.mybatis;

/**
 * 这个类用来描述一条 SQL 映射。
 *
 * 你可以把它理解成“Mapper 方法”和“数据库 SQL”之间的桥梁。
 * 一条映射至少要知道：
 * 1. 它属于哪个 namespace
 * 2. 它的方法 id 是什么
 * 3. 它真正要执行的 SQL 是什么
 * 4. 查询结果应该映射成什么类型
 * 5. 它是查询还是更新
 */
public class MappedStatement {

    /**
     * 一般对应 Mapper 接口全限定名。
     */
    private String namespace;

    /**
     * 一般对应 Mapper 接口中的方法名。
     */
    private String id;

    /**
     * 原始 SQL。
     *
     * 例如：
     * select id, name from student where id = #{id}
     */
    private String sql;

    /**
     * 查询结果映射到的 Java 类型全限定名。
     *
     * 对于 update / delete / insert 这类语句，可以为空。
     */
    private String resultType;

    /**
     * SQL 命令类型。
     */
    private SqlCommandType sqlCommandType = SqlCommandType.UNKNOWN;

    public MappedStatement() {
    }

    public MappedStatement(String namespace, String id, String sql, String resultType) {
        this.namespace = namespace;
        this.id = id;
        setSql(sql);
        this.resultType = resultType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSql() {
        return sql;
    }

    /**
     * 设置 SQL 时顺手推断命令类型，
     * 这样外部即使没显式指定，也能正常工作。
     */
    public void setSql(String sql) {
        this.sql = sql;
        if (this.sqlCommandType == SqlCommandType.UNKNOWN) {
            this.sqlCommandType = SqlCommandType.fromSql(sql);
        }
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public SqlCommandType getSqlCommandType() {
        return sqlCommandType;
    }

    public void setSqlCommandType(SqlCommandType sqlCommandType) {
        this.sqlCommandType = sqlCommandType == null ? SqlCommandType.UNKNOWN : sqlCommandType;
    }

    /**
     * 返回完整 statementId。
     */
    public String getStatementId() {
        if (namespace == null || namespace.trim().isEmpty()) {
            return id;
        }
        if (id == null || id.trim().isEmpty()) {
            return namespace;
        }
        return namespace.trim() + "." + id.trim();
    }
}
