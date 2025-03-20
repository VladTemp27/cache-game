package org.amalzen.app.util;

import java.io.*;
import java.util.Properties;

public class SessionStorage {
    private static final String SESSION_FILE = "session.properties";
    private static final Properties properties = new Properties();

    // Load session data from file
    static {
        try (FileInputStream fis = new FileInputStream(SESSION_FILE)) {
            properties.load(fis);
        } catch (IOException ignored) {
        }
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
        saveSession();
    }

    public static String get(String key) {
        return properties.getProperty(key, null);
    }

    public static void remove(String key) {
        properties.remove(key);
        saveSession();
    }

    private static void saveSession() {
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE)) {
            properties.store(fos, "Session Data");
        } catch (IOException e) {
            System.err.println("Could not save session data" + e.getMessage());
        }
    }
}
