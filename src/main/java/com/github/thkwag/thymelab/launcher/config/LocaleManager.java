package com.github.thkwag.thymelab.launcher.config;

import com.github.thkwag.thymelab.launcher.util.AppLogger;
import java.util.*;

public class LocaleManager {
    private static final String MESSAGE_BASE = "i18n/messages";
    private static List<String> supportedLanguages;
    private Locale currentLocale;
    private ResourceBundle bundle;

    private static final String[] AVAILABLE_LOCALES = {"en", "ko", "ja"};

    public LocaleManager(String lang) {
        currentLocale = Locale.ENGLISH;
        initializeSupportedLanguages();
        setLanguage(lang);
    }

    private void initializeSupportedLanguages() {
        supportedLanguages = Arrays.asList(AVAILABLE_LOCALES);
        AppLogger.info("Supported languages: " + String.join(", ", supportedLanguages));
    }

    public ResourceBundle getBundle() {
        if (bundle == null) {
            loadBundle();
        }
        return bundle;
    }

    private void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle(MESSAGE_BASE, currentLocale, this.getClass().getClassLoader());
        } catch (MissingResourceException e) {
            AppLogger.warn("Failed to load resource bundle for locale " + currentLocale + ": " + e.getMessage());
            try {
                bundle = ResourceBundle.getBundle(MESSAGE_BASE, Locale.getDefault(), this.getClass().getClassLoader());
            } catch (MissingResourceException ex) {
                AppLogger.error("Failed to load fallback resource bundle", ex);
                throw new RuntimeException("Failed to load any resource bundle", ex);
            }
        }
    }

    public void setLanguage(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            currentLocale = Locale.ENGLISH;
            bundle = null;
            return;
        }

        try {
            String langCode = lang.toLowerCase();
            if (supportedLanguages.contains(langCode)) {
                currentLocale = Locale.forLanguageTag(langCode);
            } else {
                currentLocale = Locale.ENGLISH;
            }
            bundle = null;
        } catch (Exception e) {
            AppLogger.warn("Failed to set language: " + e.getMessage());
            currentLocale = Locale.ENGLISH;
            bundle = null;
        }
    }

    public String getCurrentLanguage() {
        return currentLocale.getLanguage();
    }

    public List<String> getSupportedLanguages() {
        return Collections.unmodifiableList(supportedLanguages);
    }
} 