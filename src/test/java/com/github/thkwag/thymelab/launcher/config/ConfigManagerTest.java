package com.github.thkwag.thymelab.launcher.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    private ConfigManager configManager;
    @TempDir
    Path tempDir;
    private File configFile;
    
    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("thymelab-launcher.properties").toFile();
        configManager = new ConfigManager(configFile.getAbsolutePath());
        cleanupVersionFiles();
    }

    @Test
    @DisplayName("Return default values when config file does not exist")
    void testDefaultValuesWhenFileNotExists() {
        assertEquals("default", configManager.getProperty("test.key", "default"));
        assertEquals(42, configManager.getInt("test.int", 42));
        assertTrue(configManager.getBoolean("test.bool", true));
    }

    @Test
    @DisplayName("Test save and load properties")
    void testSaveAndLoadProperties() {
        // Given
        configManager.setProperty("string.key", "test value");
        configManager.setInt("int.key", 123);
        configManager.setBoolean("bool.key", true);
        
        // When
        configManager.save();
        configManager = new ConfigManager(configFile.getAbsolutePath());
        configManager.load();
        
        // Then
        assertEquals("test value", configManager.getProperty("string.key", ""));
        assertEquals(123, configManager.getInt("int.key", 0));
        assertTrue(configManager.getBoolean("bool.key", false));
    }

    @Test
    @DisplayName("Test handling invalid integer value")
    void testInvalidIntegerValue() {
        // Given
        configManager.setProperty("invalid.int", "not a number");
        
        // When & Then
        assertEquals(42, configManager.getInt("invalid.int", 42));
    }


    private void cleanupVersionFiles() {
        // Clean test resources
        File resourcesDir = new File("src/test/resources");
        File versionFile = new File(resourcesDir, "version.properties");
        if (versionFile.exists()) {
            versionFile.delete();
        }
        
        // Clean temp directory
        File tempVersionFile = new File(tempDir.toFile(), "version.properties");
        if (tempVersionFile.exists()) {
            tempVersionFile.delete();
        }
        File settingsFile = new File(tempDir.toFile(), "settings.gradle");
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        cleanupVersionFiles();
    }

    @ParameterizedTest
    @DisplayName("Test various boolean value formats")
    @ValueSource(strings = {"true", "TRUE", "True"})
    void testBooleanValues(String value) {
        // Given
        configManager.setProperty("test.bool", value);
        
        // Then
        assertTrue(configManager.getBoolean("test.bool", false));
    }

    @Test
    @DisplayName("Test concurrent file access")
    void testConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int num = i;
            threads[i] = new Thread(() -> {
                configManager.setProperty("key." + num, "value" + num);
                configManager.save();
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        configManager.load();
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(configManager.getProperty("key." + i, null));
        }
    }

    @Test
    @DisplayName("Test UTF-8 encoding support")
    void testUTF8Encoding() {
        // Given
        String unicodeText = "Unicode Text 유니코드";
        configManager.setProperty("unicode.text", unicodeText);
        
        // When
        configManager.save();
        configManager.load();
        
        // Then
        assertEquals(unicodeText, configManager.getProperty("unicode.text", ""));
    }
} 