package framework.springboot;

import framework.mybatis.SqlSessionFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;

/**
 * 这是整个手写 SpringBoot 框架里最核心的类。
 *
 * 你可以把它理解成一个“迷你版应用上下文”：
 * 1. 负责扫描 class
 * 2. 负责实例化 Bean
 * 3. 负责依赖注入
 * 4. 负责处理配置类里的 @MyBean
 * 5. 负责建立 URL 和控制器方法之间的映射关系
 * 6. 负责注册全局异常处理器
 * 7. 负责整合 MyBatis Mapper 和事务代理
 *
 * 面试时如果要讲这一段，可以把它概括成一句话：
 * “我自己实现了一个简化版 IoC + MVC + 事务代理 + Mapper 整合容器，
 * 启动时完成扫描、注册、注入、映射初始化和基础自动装配。”
 */
public class MyApplicationContext {

    private final Class<?> applicationClass;
    private final List<String> classNames = new ArrayList<>();
    private final Map<String, Object> beanByName = new LinkedHashMap<>();
    private final Map<String, Object> beanByType = new LinkedHashMap<>();
    private final Map<String, MyHandlerMethod> handlerMapping = new LinkedHashMap<>();
    private final Map<Class<? extends Throwable>, MyExceptionHandlerMethod> exceptionHandlerMappings = new LinkedHashMap<>();

    private MyTransactionManager transactionManager;

    public MyApplicationContext(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    public void refresh() {
        scanClasses();
        createConfigurationBeans();
        initializeTransactionManager();
        createMapperBeans();
        createComponentBeans();
        injectDependencies();
        wrapTransactionalBeans();
        injectDependencies();
        initExceptionHandlerMappings();
        initHandlerMappings();
    }

    public Map<String, MyHandlerMethod> getHandlerMapping() {
        return handlerMapping;
    }

    public <T> T getBean(Class<T> type) {
        Object bean = beanByType.get(type.getName());
        if (bean == null) {
            return null;
        }
        return type.cast(bean);
    }

    public Object getBean(String beanName) {
        return beanByName.get(beanName);
    }

    public MyExceptionHandlerMethod resolveExceptionHandler(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Class<?> current = throwable.getClass();
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) current;
            MyExceptionHandlerMethod handlerMethod = exceptionHandlerMappings.get(exceptionType);
            if (handlerMethod != null) {
                return handlerMethod;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private void scanClasses() {
        for (String basePackage : resolveBasePackages()) {
            doScan(basePackage);
        }
    }

    /**
     * 先实例化配置类，再解析其中的 @MyBean 方法。
     *
     * 这里比前一版多做了一步：
     * @MyBean 方法现在支持按类型自动注入方法参数，
     * 更接近真实 Spring 的使用方式。
     */
    private void createConfigurationBeans() {
        List<MethodDescriptor> pendingBeanMethods = new ArrayList<>();

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (!clazz.isAnnotationPresent(MyConfiguration.class)) {
                    continue;
                }

                Object configurationInstance = clazz.getDeclaredConstructor().newInstance();
                String beanName = resolveBeanName(clazz);
                registerBean(beanName, configurationInstance);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(MyBean.class)) {
                        pendingBeanMethods.add(new MethodDescriptor(configurationInstance, method));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("创建配置类 Bean 失败: " + className, e);
            }
        }

        resolvePendingBeanMethods(pendingBeanMethods);
    }

    private void resolvePendingBeanMethods(List<MethodDescriptor> pendingBeanMethods) {
        boolean progress;

        do {
            progress = false;
            Iterator<MethodDescriptor> iterator = pendingBeanMethods.iterator();
            while (iterator.hasNext()) {
                MethodDescriptor descriptor = iterator.next();
                Object[] args = tryResolveMethodArguments(descriptor.method);
                if (args == null) {
                    continue;
                }

                try {
                    descriptor.method.setAccessible(true);
                    Object bean = descriptor.method.invoke(descriptor.owner, args);
                    if (bean == null) {
                        throw new IllegalStateException("@MyBean 方法返回值不能为空: " + descriptor.method.getName());
                    }

                    MyBean myBean = descriptor.method.getAnnotation(MyBean.class);
                    String beanName = myBean.value().trim();
                    if (beanName.isEmpty()) {
                        beanName = descriptor.method.getName();
                    }

                    registerBean(beanName, bean);
                    iterator.remove();
                    progress = true;
                } catch (Exception e) {
                    throw new RuntimeException("创建 @MyBean Bean 失败: " + descriptor.method.getName(), e);
                }
            }
        } while (progress);

        if (!pendingBeanMethods.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (MethodDescriptor descriptor : pendingBeanMethods) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(descriptor.method.getDeclaringClass().getSimpleName())
                        .append(".")
                        .append(descriptor.method.getName());
            }
            throw new RuntimeException("@MyBean 方法存在无法解析的依赖: " + builder);
        }
    }

    private void initializeTransactionManager() {
        SqlSessionFactory sqlSessionFactory = getBean(SqlSessionFactory.class);
        if (sqlSessionFactory != null) {
            transactionManager = new MyTransactionManager(sqlSessionFactory);
        }
    }

    /**
     * 创建 Mapper 接口代理 Bean。
     *
     * 只有当容器里已经存在 SqlSessionFactory 时，
     * 这一步才有意义。
     */
    private void createMapperBeans() {
        SqlSessionFactory sqlSessionFactory = getBean(SqlSessionFactory.class);
        if (sqlSessionFactory == null) {
            return;
        }

        Set<String> mapperPackages = resolveMapperPackages();
        boolean hasMapperScan = !mapperPackages.isEmpty();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (!clazz.isInterface()) {
                    continue;
                }

                boolean annotatedMapper = clazz.isAnnotationPresent(MyMapper.class);
                boolean matchedMapperScan = hasMapperScan && isInMapperPackages(clazz, mapperPackages);
                if (!annotatedMapper && !matchedMapperScan) {
                    continue;
                }

                if (beanByType.containsKey(clazz.getName())) {
                    continue;
                }

                Object proxy = Proxy.newProxyInstance(
                        clazz.getClassLoader(),
                        new Class[]{clazz},
                        new MyManagedMapperProxy(clazz, sqlSessionFactory, transactionManager)
                );

                String beanName = resolveMapperBeanName(clazz);
                registerBean(beanName, proxy);
            } catch (Exception e) {
                throw new RuntimeException("创建 Mapper Bean 失败: " + className, e);
            }
        }
    }

    private void createComponentBeans() {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isInterface()) {
                    continue;
                }
                if (!isManagedComponent(clazz) || clazz.isAnnotationPresent(MyConfiguration.class)) {
                    continue;
                }

                String beanName = resolveBeanName(clazz);
                if (beanByName.containsKey(beanName)) {
                    continue;
                }

                Object instance = clazz.getDeclaredConstructor().newInstance();
                registerBean(beanName, instance);
            } catch (Exception e) {
                throw new RuntimeException("创建组件失败: " + className, e);
            }
        }
    }

    /**
     * 对声明了 @MyTransactional 的 Bean 进行代理包装。
     *
     * 这里用的是 JDK 动态代理，所以只支持“实现了接口”的 Bean。
     * 这和真实 Spring 在默认 JDK 代理模式下的限制是类似的。
     */
    private void wrapTransactionalBeans() {
        if (transactionManager == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : beanByName.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            if (!requiresTransactionalProxy(beanClass)) {
                continue;
            }

            Class<?>[] interfaces = beanClass.getInterfaces();
            if (interfaces.length == 0) {
                continue;
            }

            Object proxy = Proxy.newProxyInstance(
                    beanClass.getClassLoader(),
                    interfaces,
                    new MyTransactionalInvocationHandler(bean, transactionManager)
            );

            // 按名称替换，这样后续按名称注入也能拿到代理。
            entry.setValue(proxy);

            // 对接口类型的注入统一指向代理。
            for (Class<?> interfaceType : interfaces) {
                beanByType.put(interfaceType.getName(), proxy);
            }
        }
    }

    private void injectDependencies() {
        for (Object bean : beanByName.values()) {
            // JDK 动态代理没有具体字段，不需要做字段注入
            if (Proxy.isProxyClass(bean.getClass())) {
                continue;
            }

            Field[] fields = bean.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                Object dependency = resolveDependency(field, autowired);
                if (dependency == null) {
                    throw new RuntimeException("依赖注入失败，找不到 Bean: " + field.getType().getName());
                }

                try {
                    field.setAccessible(true);
                    field.set(bean, dependency);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("字段注入失败: " + field.getName(), e);
                }
            }
        }
    }

    private void initExceptionHandlerMappings() {
        for (Object bean : beanByName.values()) {
            Class<?> clazz = unwrapProxyClass(bean.getClass());
            if (!clazz.isAnnotationPresent(MyControllerAdvice.class)) {
                continue;
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(MyExceptionHandler.class)) {
                    continue;
                }

                Class<? extends Throwable>[] exceptionTypes = resolveExceptionTypes(method);
                if (exceptionTypes.length == 0) {
                    throw new RuntimeException("@MyExceptionHandler 必须声明异常类型: " + method.getName());
                }

                for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                    if (exceptionHandlerMappings.containsKey(exceptionType)) {
                        throw new RuntimeException("重复的全局异常处理器: " + exceptionType.getName());
                    }
                    method.setAccessible(true);
                    exceptionHandlerMappings.put(exceptionType, new MyExceptionHandlerMethod(exceptionType, bean, method));
                }
            }
        }
    }

    private void initHandlerMappings() {
        for (Object bean : beanByName.values()) {
            Class<?> clazz = unwrapProxyClass(bean.getClass());
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                baseUrl = normalizeUrl(clazz.getAnnotation(MyRequestMapping.class).value());
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String methodUrl = normalizeUrl(requestMapping.value());
                String finalUrl = normalizeUrl(baseUrl + "/" + methodUrl);
                String httpMethod = requestMapping.method().trim().toUpperCase(Locale.ROOT);
                String mappingKey = buildMappingKey(httpMethod, finalUrl);

                if (handlerMapping.containsKey(mappingKey)) {
                    throw new RuntimeException("重复的请求映射: " + mappingKey);
                }

                handlerMapping.put(mappingKey, new MyHandlerMethod(httpMethod, finalUrl, bean, method));
            }
        }
    }

    private Object resolveDependency(Field field, MyAutowired autowired) {
        String beanName = autowired.value().trim();
        if (!beanName.isEmpty()) {
            return beanByName.get(beanName);
        }

        Object dependency = beanByType.get(field.getType().getName());
        if (dependency != null) {
            return dependency;
        }

        return beanByName.get(field.getName());
    }

    private boolean isManagedComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(MyComponent.class)
                || clazz.isAnnotationPresent(MyController.class)
                || clazz.isAnnotationPresent(MyService.class)
                || clazz.isAnnotationPresent(MyConfiguration.class)
                || clazz.isAnnotationPresent(MyControllerAdvice.class);
    }

    private String resolveBeanName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(MyComponent.class)) {
            String value = clazz.getAnnotation(MyComponent.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (clazz.isAnnotationPresent(MyController.class)) {
            String value = clazz.getAnnotation(MyController.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (clazz.isAnnotationPresent(MyService.class)) {
            String value = clazz.getAnnotation(MyService.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (clazz.isAnnotationPresent(MyConfiguration.class)) {
            String value = clazz.getAnnotation(MyConfiguration.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return toLowerFirstCase(clazz.getSimpleName());
    }

    private String resolveMapperBeanName(Class<?> mapperInterface) {
        if (mapperInterface.isAnnotationPresent(MyMapper.class)) {
            String value = mapperInterface.getAnnotation(MyMapper.class).value().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return toLowerFirstCase(mapperInterface.getSimpleName());
    }

    private void registerBean(String beanName, Object bean) {
        if (beanByName.containsKey(beanName) && beanByName.get(beanName) != bean) {
            throw new RuntimeException("重复的 Bean 名称: " + beanName);
        }

        beanByName.put(beanName, bean);
        registerType(bean.getClass().getName(), bean);

        Class<?> beanClass = bean.getClass();
        for (Class<?> interfaceType : beanClass.getInterfaces()) {
            registerType(interfaceType.getName(), bean);
        }

        Class<?> current = beanClass.getSuperclass();
        while (current != null && current != Object.class) {
            registerType(current.getName(), bean);
            current = current.getSuperclass();
        }
    }

    private void registerType(String typeName, Object bean) {
        if (beanByType.containsKey(typeName) && beanByType.get(typeName) != bean) {
            throw new RuntimeException("同一类型存在多个 Bean，实现冲突: " + typeName);
        }
        beanByType.put(typeName, bean);
    }

    private String[] resolveBasePackages() {
        MySpringBootApplication application = applicationClass.getAnnotation(MySpringBootApplication.class);
        if (application == null) {
            throw new RuntimeException("启动类缺少 @MySpringBootApplication: " + applicationClass.getName());
        }

        String[] basePackages = application.scanBasePackages();
        if (basePackages.length > 0) {
            return basePackages;
        }

        Package applicationPackage = applicationClass.getPackage();
        if (applicationPackage == null) {
            throw new RuntimeException("无法解析启动类所在包: " + applicationClass.getName());
        }
        return new String[]{applicationPackage.getName()};
    }

    private Set<String> resolveMapperPackages() {
        LinkedHashSet<String> mapperPackages = new LinkedHashSet<>();
        MyMapperScan mapperScan = applicationClass.getAnnotation(MyMapperScan.class);
        if (mapperScan != null && mapperScan.value().length > 0) {
            mapperPackages.addAll(Arrays.asList(mapperScan.value()));
        }
        return mapperPackages;
    }

    private boolean isInMapperPackages(Class<?> clazz, Set<String> mapperPackages) {
        String className = clazz.getName();
        for (String mapperPackage : mapperPackages) {
            if (className.startsWith(mapperPackage + ".") || className.equals(mapperPackage)) {
                return true;
            }
        }
        return false;
    }

    private void doScan(String basePackage) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(basePackage.replace('.', '/'));
        if (url == null) {
            return;
        }

        File dir = new File(url.getFile());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                doScan(basePackage + "." + file.getName());
                continue;
            }

            if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().replace(".class", "");
                if (!classNames.contains(className)) {
                    classNames.add(className);
                }
            }
        }
    }

    private Object[] tryResolveMethodArguments(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Object dependency = beanByType.get(parameterTypes[i].getName());
            if (dependency == null) {
                return null;
            }
            args[i] = dependency;
        }
        return args;
    }

    private boolean requiresTransactionalProxy(Class<?> beanClass) {
        if (beanClass.isAnnotationPresent(MyTransactional.class)) {
            return true;
        }
        for (Method method : beanClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MyTransactional.class)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] resolveExceptionTypes(Method method) {
        MyExceptionHandler exceptionHandler = method.getAnnotation(MyExceptionHandler.class);
        if (exceptionHandler.value().length > 0) {
            return exceptionHandler.value();
        }

        List<Class<? extends Throwable>> exceptionTypes = new ArrayList<>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (Throwable.class.isAssignableFrom(parameterType)) {
                exceptionTypes.add((Class<? extends Throwable>) parameterType);
            }
        }
        return exceptionTypes.toArray(new Class[0]);
    }

    private Class<?> unwrapProxyClass(Class<?> clazz) {
        if (Proxy.isProxyClass(clazz) && clazz.getInterfaces().length > 0) {
            return clazz.getInterfaces()[0];
        }
        return clazz;
    }

    private String normalizeUrl(String url) {
        String normalized = (url == null ? "" : url.trim()).replaceAll("/+", "/");
        if (normalized.isEmpty()) {
            return "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replaceAll("/+", "/");
    }

    private String buildMappingKey(String httpMethod, String url) {
        return httpMethod + ":" + url;
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }

    private static class MethodDescriptor {
        private final Object owner;
        private final Method method;

        private MethodDescriptor(Object owner, Method method) {
            this.owner = owner;
            this.method = method;
        }
    }
}
