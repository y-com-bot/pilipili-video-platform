package com.yuan.utils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;

public class ConfigManager {

    private static final Properties PROPERTIES = new Properties();
    private static volatile ConfigService nacosConfigService;

    static {
        loadLocalProperties();
        bootstrapNacosConfig();
    }

    public static synchronized String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static synchronized String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static synchronized void setProperty(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (value == null) {
            PROPERTIES.remove(key);
        } else {
            PROPERTIES.setProperty(key, value);
        }
    }

    public static synchronized Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(PROPERTIES);
        return copy;
    }

    public static synchronized void overlay(Properties overrides) {
        if (overrides == null) {
            return;
        }
        for (String key : overrides.stringPropertyNames()) {
            PROPERTIES.setProperty(key, overrides.getProperty(key));
        }
    }

    public static ConfigService getNacosConfigService() {
        return nacosConfigService;
    }

    public static synchronized String dumpAsPropertiesContent() {
        try {
            StringWriter writer = new StringWriter();
            PROPERTIES.store(writer, "video-platform");
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Serialize properties failed", e);
        }
    }

    private static void loadLocalProperties() {
        try (InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream("ab.properties")) {
            if (inputStream == null) {
                AppLogger.getLogger().warning("ab.properties not found");
                return;
            }
            PROPERTIES.load(inputStream);
            AppLogger.getLogger().info("ab.properties loaded");
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Load ab.properties failed", e);
        }
    }

    private static void bootstrapNacosConfig() {
        if (!Boolean.parseBoolean(getProperty("nacos.enabled", "false"))) {
            return;
        }

        try {
            Properties nacosProperties = new Properties();
            nacosProperties.put("serverAddr", getProperty("nacos.server-addr", "127.0.0.1:8848"));
            putIfHasText(nacosProperties, "namespace", getProperty("nacos.namespace", ""));
            putIfHasText(nacosProperties, "username", getProperty("nacos.username", ""));
            putIfHasText(nacosProperties, "password", getProperty("nacos.password", ""));

            String dataId = getProperty("nacos.config.data-id", "video-platform.properties");
            String group = getProperty("nacos.config.group", "DEFAULT_GROUP");
            long timeoutMs = Long.parseLong(getProperty("nacos.config.timeout-ms", "3000"));

            ConfigService configService = NacosFactory.createConfigService(nacosProperties);
            nacosConfigService = configService;

            String remoteContent = configService.getConfig(dataId, group, timeoutMs);
            if (remoteContent == null || remoteContent.isBlank()) {
                if (Boolean.parseBoolean(getProperty("nacos.bootstrap.publish", "false"))) {
                    configService.publishConfig(dataId, group, dumpAsPropertiesContent());
                    AppLogger.getLogger().info("Published bootstrap configuration to Nacos");
                }
            } else {
                overlay(parseProperties(remoteContent));
                AppLogger.getLogger().info("Loaded remote configuration from Nacos");
            }

            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    overlay(parseProperties(configInfo));
                    AppLogger.getLogger().info("Refreshed configuration from Nacos listener");
                }
            });
        } catch (NacosException e) {
            AppLogger.getLogger().log(Level.WARNING, "Bootstrap Nacos configuration failed", e);
        }
    }

    private static Properties parseProperties(String raw) {
        Properties parsed = new Properties();
        if (raw == null || raw.isBlank()) {
            return parsed;
        }
        try (StringReader reader = new StringReader(raw)) {
            parsed.load(reader);
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Parse remote properties failed", e);
        }
        return parsed;
    }

    private static void putIfHasText(Properties target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
