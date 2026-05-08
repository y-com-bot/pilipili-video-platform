package mybatis;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 最基础的执行器实现。
 *
 * 它专注做三件事：
 * 1. 把带 #{...} 的 SQL 解析成 JDBC 能执行的 ? 占位符 SQL
 * 2. 把方法参数安全地绑定到 PreparedStatement 中
 * 3. 把 ResultSet 映射回 Java 对象
 *
 * 这是整个手写 MyBatis 里“最接近底层执行”的一层。
 */
public class SimpleExecutor implements Executor {

    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{\\s*([\\w.]+)\\s*}");

    private final Transaction transaction;

    public SimpleExecutor(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public int update(MappedStatement mappedStatement, Object parameter) throws Exception {
        Connection connection = transaction.getConnection();
        BoundSql boundSql = parseSql(mappedStatement.getSql());

        try (PreparedStatement preparedStatement = connection.prepareStatement(boundSql.getJdbcSql())) {
            setParameters(preparedStatement, boundSql, parameter);
            return preparedStatement.executeUpdate();
        }
    }

    @Override
    public <T> List<T> query(MappedStatement mappedStatement, Object parameter) throws Exception {
        Connection connection = transaction.getConnection();
        BoundSql boundSql = parseSql(mappedStatement.getSql());

        try (PreparedStatement preparedStatement = connection.prepareStatement(boundSql.getJdbcSql())) {
            setParameters(preparedStatement, boundSql, parameter);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return handleResultSet(resultSet, mappedStatement.getResultType());
            }
        }
    }

    /**
     * 把 SQL 中的 #{id} 解析成 ?，
     * 同时记录参数名顺序，后面绑定参数时要用到。
     */
    private BoundSql parseSql(String originalSql) {
        if (originalSql == null || originalSql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        List<String> parameterNames = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(originalSql);
        StringBuffer jdbcSql = new StringBuffer();

        while (matcher.find()) {
            parameterNames.add(matcher.group(1));
            matcher.appendReplacement(jdbcSql, "?");
        }
        matcher.appendTail(jdbcSql);

        return new BoundSql(jdbcSql.toString(), parameterNames);
    }

    /**
     * 结果集映射。
     *
     * 这里支持三种常见场景：
     * 1. resultType 为空：每一行返回一个 Map
     * 2. resultType 是基础类型 / 包装类型 / String：取第一列
     * 3. resultType 是实体类：按字段名反射赋值
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> handleResultSet(ResultSet resultSet, String resultType) throws Exception {
        List<T> results = new ArrayList<>();

        if (resultType == null || resultType.trim().isEmpty()) {
            while (resultSet.next()) {
                results.add((T) mapRowToMap(resultSet));
            }
            return results;
        }

        Class<?> resultClass = Class.forName(resultType.trim());
        if (isSimpleType(resultClass)) {
            while (resultSet.next()) {
                Object value = resultSet.getObject(1);
                results.add((T) adaptSimpleValue(resultClass, value));
            }
            return results;
        }

        while (resultSet.next()) {
            Object instance = resultClass.getDeclaredConstructor().newInstance();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i);
                Object columnValue = resultSet.getObject(i);
                setFieldValue(instance, columnLabel, columnValue);
            }

            results.add((T) instance);
        }

        return results;
    }

    private Map<String, Object> mapRowToMap(ResultSet resultSet) throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
        }
        return row;
    }

    /**
     * 参数绑定。
     *
     * 推荐使用 #{...} 形式的命名占位符，
     * 这样参数和 SQL 的对应关系会非常清晰。
     *
     * 如果 SQL 里本来就只有普通 ?，这里也保留了一个兜底策略，
     * 让简单场景不至于直接跑不通。
     */
    private void setParameters(PreparedStatement preparedStatement, BoundSql boundSql, Object parameter) throws Exception {
        List<String> parameterNames = boundSql.getParameterNames();

        if (!parameterNames.isEmpty()) {
            for (int i = 0; i < parameterNames.size(); i++) {
                Object value = resolveParameterValue(parameter, parameterNames.get(i), i);
                preparedStatement.setObject(i + 1, value);
            }
            return;
        }

        // 如果原 SQL 就是普通 ? 占位符，走保底绑定逻辑
        bindFallbackParameters(preparedStatement, parameter);
    }

    private void bindFallbackParameters(PreparedStatement preparedStatement, Object parameter) throws Exception {
        if (parameter == null) {
            return;
        }

        if (isSimpleType(parameter.getClass())) {
            preparedStatement.setObject(1, parameter);
            return;
        }

        if (parameter instanceof Map<?, ?> parameterMap) {
            int index = 1;
            for (Object value : parameterMap.values()) {
                preparedStatement.setObject(index++, value);
            }
            return;
        }

        Field[] fields = parameter.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            preparedStatement.setObject(i + 1, fields[i].get(parameter));
        }
    }

    /**
     * 根据参数名解析实际值。
     *
     * 支持：
     * 1. 单个基础类型参数
     * 2. Map 参数
     * 3. JavaBean 参数
     * 4. 嵌套属性写法，例如 #{user.id}
     */
    private Object resolveParameterValue(Object parameter, String parameterName, int parameterIndex) throws Exception {
        if (parameter == null) {
            return null;
        }

        if (isSimpleType(parameter.getClass())) {
            return parameter;
        }

        if (parameter instanceof Map<?, ?> parameterMap) {
            if (parameterMap.containsKey(parameterName)) {
                return parameterMap.get(parameterName);
            }
            if (parameterMap.containsKey("param" + (parameterIndex + 1))) {
                return parameterMap.get("param" + (parameterIndex + 1));
            }
            if (parameterMap.containsKey("arg" + parameterIndex)) {
                return parameterMap.get("arg" + parameterIndex);
            }
            return null;
        }

        return getPropertyValue(parameter, parameterName);
    }

    private Object getPropertyValue(Object bean, String propertyPath) throws Exception {
        String[] parts = propertyPath.split("\\.");
        Object current = bean;

        for (String part : parts) {
            if (current == null) {
                return null;
            }
            Field field = findField(current.getClass(), part);
            if (field == null) {
                throw new RuntimeException("参数对象中不存在属性: " + propertyPath);
            }
            field.setAccessible(true);
            current = field.get(current);
        }
        return current;
    }

    private void setFieldValue(Object instance, String columnName, Object columnValue) throws Exception {
        Field field = findField(instance.getClass(), columnName);
        if (field == null) {
            field = findField(instance.getClass(), underlineToCamel(columnName));
        }
        if (field == null) {
            return;
        }

        field.setAccessible(true);
        field.set(instance, adaptObjectValue(field.getType(), columnValue));
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class
                || java.util.Date.class.isAssignableFrom(type)
                || java.sql.Date.class.isAssignableFrom(type)
                || java.sql.Timestamp.class.isAssignableFrom(type);
    }

    private Object adaptSimpleValue(Class<?> targetType, Object value) {
        return adaptObjectValue(targetType, value);
    }

    /**
     * 处理 JDBC 返回值和 Java 字段类型之间的常见类型适配。
     */
    private Object adaptObjectValue(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if ((targetType == int.class || targetType == Integer.class) && value instanceof Number number) {
            return number.intValue();
        }
        if ((targetType == long.class || targetType == Long.class) && value instanceof Number number) {
            return number.longValue();
        }
        if ((targetType == double.class || targetType == Double.class) && value instanceof Number number) {
            return number.doubleValue();
        }
        if ((targetType == float.class || targetType == Float.class) && value instanceof Number number) {
            return number.floatValue();
        }
        if ((targetType == short.class || targetType == Short.class) && value instanceof Number number) {
            return number.shortValue();
        }
        if ((targetType == byte.class || targetType == Byte.class) && value instanceof Number number) {
            return number.byteValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        if ((targetType == char.class || targetType == Character.class) && value instanceof String stringValue && !stringValue.isEmpty()) {
            return stringValue.charAt(0);
        }
        return value;
    }

    private String underlineToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;

        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                builder.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }

    /**
     * 一个很小但很实用的内部对象，
     * 用来保存“解析后的 JDBC SQL”和“参数名顺序”。
     */
    private static class BoundSql {
        private final String jdbcSql;
        private final List<String> parameterNames;

        private BoundSql(String jdbcSql, List<String> parameterNames) {
            this.jdbcSql = jdbcSql;
            this.parameterNames = parameterNames;
        }

        public String getJdbcSql() {
            return jdbcSql;
        }

        public List<String> getParameterNames() {
            return parameterNames;
        }
    }
}
