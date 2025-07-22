package com.babsnet.posapp.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigUtil {
    private static final Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        String configPath = Paths.get(System.getProperty("user.dir"), "config.properties").toString();
        try (InputStream is = new FileInputStream(configPath)) {
            props.clear();
            props.load(is);
        } catch (Exception e) {
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) {
                    props.clear();
                    props.load(is);
                } else {
                    throw new RuntimeException("config.properties not found!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Failed to load config.properties", ex);
            }
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static void reload() {
        load();
    }
}

