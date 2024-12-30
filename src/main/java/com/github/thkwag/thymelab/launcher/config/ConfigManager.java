package com.github.thkwag.thymelab.launcher.config;

import com.github.thkwag.thymelab.launcher.util.AppLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final Properties props = new Properties();
    private final String path;
    private final String version = loadVersion();
    private final List<LanguageChangeListener> listeners = new ArrayList<>();
    private final LocaleManager localeManager;

    // Version related constants
    private static final String VERSION_NOT_FOUND = "not-found";
    private static final String VERSION_FILE_NAME = "version.properties";
    private static final String VERSION_PROPERTY_KEY = "version";
    private static final String GRADLE_SETTINGS_FILE = "settings.gradle";
    private static final String PROCESSOR_JAR_PATH = "processor.jar.path";

    public ConfigManager(String path) {
        this.path = path;
        this.localeManager = new LocaleManager(getProperty("language", "en"));
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public void addLanguageChangeListener(LanguageChangeListener listener) {
        listeners.add(listener);
    }

    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyLanguageChange(String languageCode) {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChange(languageCode);
        }
    }

    private String loadVersion() {
        String version = loadVersionFromClasspath();
        if (!VERSION_NOT_FOUND.equals(version)) {
            return version;
        }
        
        version = loadVersionFromProjectRoot();
        return version;
    }

    private String loadVersionFromClasspath() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(VERSION_FILE_NAME)) {
            if (is != null) {
                Properties versionProps = new Properties();
                versionProps.load(is);
                return versionProps.getProperty(VERSION_PROPERTY_KEY, VERSION_NOT_FOUND).trim();
            }
        } catch (IOException e) {
            // ignore
        }
        return VERSION_NOT_FOUND;
    }

    private String loadVersionFromProjectRoot() {
        try {
            File currentLocation = new File(getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            
            File projectRoot = findProjectRoot(currentLocation);
            if (projectRoot != null) {
                return loadVersionFromFile(new File(projectRoot, VERSION_FILE_NAME));
            }
        } catch (Exception e) {
            // ignore
        }
        return VERSION_NOT_FOUND;
    }

    private File findProjectRoot(File start) {
        File current = start;
        while (current != null && !new File(current, GRADLE_SETTINGS_FILE).exists()) {
            current = current.getParentFile();
        }
        return current;
    }

    private String loadVersionFromFile(File versionFile) {
        if (versionFile.exists()) {
            try (FileInputStream fis = new FileInputStream(versionFile)) {
                Properties versionProps = new Properties();
                versionProps.load(fis);
                return versionProps.getProperty(VERSION_PROPERTY_KEY, VERSION_NOT_FOUND).trim();
            } catch (IOException e) {
                // ignore
            }
        }
        return VERSION_NOT_FOUND;
    }

    public void load() {
        if (Files.exists(Paths.get(path))) {
            try (InputStream in = Files.newInputStream(Paths.get(path))) {
                props.load(in);
            } catch (IOException e) {
                AppLogger.error("Failed to load configuration", e);
            }
        }
    }

    public void save() {
        try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
            props.store(out, "Application Configuration");
        } catch (IOException e) {
            AppLogger.error("Failed to save configuration", e);
        }
    }

    public String getProperty(String key, String def) {
        return props.getProperty(key, def);
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void setInt(String key, int val) {
        setProperty(key, String.valueOf(val));
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getProperty(key, String.valueOf(def)));
    }

    public void setBoolean(String key, boolean val) {
        setProperty(key, String.valueOf(val));
    }

    public void loadProperties() {
        load();
    }

    public String getVersion() {
        return version;
    }

    public void changeLanguage(String languageCode) {
        setProperty("language", languageCode);
        localeManager.setLanguage(languageCode);
        save();
        notifyLanguageChange(languageCode);
    }

    public String getProcessorJarPath() {
        return props.getProperty(PROCESSOR_JAR_PATH, "");
    }

    public void setProcessorJarPath(String path) {
        props.setProperty(PROCESSOR_JAR_PATH, path);
        save();
    }

    public interface LanguageChangeListener {
        void onLanguageChange(String languageCode);
    }
} 