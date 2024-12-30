package com.github.thkwag.thymelab.launcher.process;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.config.LocaleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppProcessManagerTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private LogConsumer logConsumer;
    
    @Mock
    private LocaleManager localeManager;
    
    @Mock
    private ResourceBundle resourceBundle;
    
    private AppProcessManager processManager;
    private AtomicBoolean processExited;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processExited = new AtomicBoolean(false);
        processManager = new AppProcessManager(
            logConsumer,
            () -> processExited.set(true),
            configManager
        );
        
        // Mock basic configuration
        when(configManager.getVersion()).thenReturn("1.0.0");
        when(configManager.getProperty("log.level", "INFO")).thenReturn("INFO");
        when(configManager.getInt("server.port", 8080)).thenReturn(8080);
        when(configManager.getProperty("static.folder.path", "")).thenReturn("");
        when(configManager.getProperty("templates.folder.path", "")).thenReturn("");
        when(configManager.getProperty("data.folder.path", "")).thenReturn("");
        
        // Mock processor jar path and locale manager
        when(configManager.getProcessorJarPath()).thenReturn("");
        when(configManager.getLocaleManager()).thenReturn(localeManager);
        when(localeManager.getBundle()).thenReturn(resourceBundle);
        when(resourceBundle.getString("jar_not_configured")).thenReturn("No JAR configured");
    }

    @Test
    @DisplayName("Test initial state")
    void testInitialState() {
        assertFalse(processManager.isRunning());
    }

    @Test
    @DisplayName("Test configuration loading")
    void testConfigurationLoading() {
        // When
        processManager.startProcess();
        
        // Then
        verify(configManager, atLeastOnce()).getProcessorJarPath();
        verify(configManager, atLeastOnce()).getLocaleManager();
        verify(logConsumer, atLeastOnce()).accept(anyString());
    }

    @Test
    @DisplayName("Test process stop")
    void testProcessStop() {
        processManager.stopProcess();
        assertFalse(processManager.isRunning());
    }

    @Test
    @DisplayName("Test log consumer")
    void testLogConsumer() {
        processManager.startProcess();
        verify(logConsumer, atLeastOnce()).accept(anyString());
    }
} 