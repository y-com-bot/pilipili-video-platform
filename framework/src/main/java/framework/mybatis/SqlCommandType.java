package framework.mybatis;

import java.util.Locale;

/**
 * SQL 命令类型枚举。
 *
 * 真实 MyBatis 里也有类似概念，
 * 因为框架需要知道当前方法执行的是查询还是更新，
 * 这样才能路由到不同的执行逻辑。
 */
public enum SqlCommandType {
    UNKNOWN,
    SELECT,
    INSERT,
    UPDATE,
    DELETE;

    public static SqlCommandType fromSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalized = sql.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("SELECT")) {
            return SELECT;
        }
        if (normalized.startsWith("INSERT")) {
            return INSERT;
        }
        if (normalized.startsWith("UPDATE")) {
            return UPDATE;
        }
        if (normalized.startsWith("DELETE")) {
            return DELETE;
        }
        return UNKNOWN;
    }
}
