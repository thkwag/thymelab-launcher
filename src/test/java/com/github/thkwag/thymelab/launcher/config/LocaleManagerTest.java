package com.github.thkwag.thymelab.launcher.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

class LocaleManagerTest {
    private LocaleManager localeManager;

    @BeforeEach
    void setUp() {
        localeManager = new LocaleManager("en");
    }

    @Test
    @DisplayName("Test default English locale setting")
    void testDefaultEnglishLocale() {
        assertEquals("en", localeManager.getCurrentLanguage());
        ResourceBundle bundle = localeManager.getBundle();
        assertNotNull(bundle);
    }

    @Test
    @DisplayName("Test Korean locale setting")
    void testKoreanLocale() {
        localeManager.setLanguage("ko");
        assertEquals("ko", localeManager.getCurrentLanguage());
        ResourceBundle bundle = localeManager.getBundle();
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @DisplayName("Test case-insensitive language code handling")
    @ValueSource(strings = {"KO", "ko", "Ko", "kO"})
    void testCaseInsensitiveLanguageCode(String lang) {
        localeManager.setLanguage(lang);
        assertEquals("ko", localeManager.getCurrentLanguage());
    }

    @Test
    @DisplayName("Test fallback to English for invalid language code")
    void testFallbackToEnglishForInvalidLanguage() {
        localeManager.setLanguage("invalid");
        assertEquals("en", localeManager.getCurrentLanguage());
    }

    @Test
    @DisplayName("Test bundle caching")
    void testBundleCaching() {
        ResourceBundle bundle1 = localeManager.getBundle();
        ResourceBundle bundle2 = localeManager.getBundle();
        assertSame(bundle1, bundle2);
    }

    @Test
    @DisplayName("Test bundle reloading on language change")
    void testBundleReloadingOnLanguageChange() {
        ResourceBundle enBundle = localeManager.getBundle();
        localeManager.setLanguage("ko");
        ResourceBundle koBundle = localeManager.getBundle();
        assertNotSame(enBundle, koBundle);
    }
} 