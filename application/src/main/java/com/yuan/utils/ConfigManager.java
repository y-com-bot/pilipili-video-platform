package com.yuan.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Properties properties = new Properties();
    static{
        try(InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream("ab.properties")) {
            if (inputStream == null){
                System.err.println("配置文件 ab.properties 不存在");
            }
            else{
                properties.load(inputStream);
                System.out.println("配置文件 ab.properties 加载成功");
            }
        } catch (IOException e) {
            System.err.println("加载配置文件 ab.properties 失败");
            e.printStackTrace();
        }
    }
    public static String getProperty(String key){
        return properties.getProperty(key);
    }
    public static String getProperty(String key, String defaultValue){
        return properties.getProperty(key, defaultValue);
    }

}
