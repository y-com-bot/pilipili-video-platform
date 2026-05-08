package mybatis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper 动态代理。
 *
 * 它是“接口调用”和“SQL 执行”之间最关键的一层桥梁。
 * 当我们写：
 *
 * StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
 * mapper.getById(1);
 *
 * 实际上真正进入的就是这里的 invoke 方法。
 */
public class MapperProxy implements InvocationHandler {

    private final SqlSession sqlSession;
    private final Class<?> mapperInterface;
    private final Configuration configuration;

    public MapperProxy(SqlSession sqlSession, Class<?> mapperInterface, Configuration configuration) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.configuration = configuration;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args);
        }

        String statementId = mapperInterface.getName() + "." + method.getName();
        MappedStatement mappedStatement = configuration.getMappedStatement(statementId);
        if (mappedStatement == null) {
            throw new RuntimeException("找不到 Mapper 方法对应的 SQL 映射: " + statementId);
        }

        Object parsedParameter = parseParameters(method, args);
        SqlCommandType commandType = mappedStatement.getSqlCommandType();

        if (commandType == SqlCommandType.SELECT || commandType == SqlCommandType.UNKNOWN) {
            return executeQuery(statementId, method, parsedParameter);
        }

        return executeUpdate(statementId, method, parsedParameter, commandType);
    }

    private Object executeQuery(String statementId, Method method, Object parameter) throws Exception {
        if (List.class.isAssignableFrom(method.getReturnType())) {
            return sqlSession.selectList(statementId, parameter);
        }
        return sqlSession.selectOne(statementId, parameter);
    }

    private Object executeUpdate(String statementId, Method method, Object parameter, SqlCommandType commandType) throws Exception {
        int affectedRows;
        switch (commandType) {
            case INSERT:
                affectedRows = sqlSession.insert(statementId, parameter);
                break;
            case UPDATE:
                affectedRows = sqlSession.update(statementId, parameter);
                break;
            case DELETE:
                affectedRows = sqlSession.delete(statementId, parameter);
                break;
            default:
                throw new RuntimeException("不支持的 SQL 类型: " + commandType);
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return null;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return affectedRows;
        }
        if (returnType == long.class || returnType == Long.class) {
            return (long) affectedRows;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return affectedRows > 0;
        }

        throw new RuntimeException("增删改方法的返回类型暂不支持: " + returnType.getName());
    }

    /**
     * 参数解析规则：
     * 1. 无参方法 -> null
     * 2. 单参数且未使用 @Param -> 直接传原始对象
     * 3. 多参数或使用了 @Param -> 组装成 Map
     *
     * 这里用 LinkedHashMap 是有意为之，
     * 因为它能保持参数放入顺序，后面在某些兜底绑定场景里更稳定。
     */
    private Object parseParameters(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        Parameter[] parameters = method.getParameters();
        boolean hasParamAnnotation = false;
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(Param.class)) {
                hasParamAnnotation = true;
                break;
            }
        }

        if (args.length == 1 && !hasParamAnnotation) {
            return args[0];
        }

        Map<String, Object> paramMap = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = args[i];

            Param paramAnnotation = parameter.getAnnotation(Param.class);
            if (paramAnnotation != null && !paramAnnotation.value().trim().isEmpty()) {
                paramMap.put(paramAnnotation.value().trim(), value);
            }

            // 这些保底 key 能提高可用性：
            // - arg0 / arg1：不依赖编译参数
            // - param1 / param2：更接近 MyBatis 的习惯
            // - parameter.getName()：如果编译时加了 -parameters 会更可读
            paramMap.put("arg" + i, value);
            paramMap.put("param" + (i + 1), value);
            paramMap.put(parameter.getName(), value);
        }
        return paramMap;
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        if ("toString".equals(methodName)) {
            return mapperInterface.getName() + " Mapper Proxy";
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(methodName)) {
            return proxy == args[0];
        }
        throw new UnsupportedOperationException("不支持的 Object 方法: " + methodName);
    }
}
