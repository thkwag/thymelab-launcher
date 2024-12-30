package com.github.thkwag.thymelab.launcher.process;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppProcessManagerTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private LogConsumer logConsumer;
    
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
        
        when(configManager.getVersion()).thenReturn("1.0.0");
        when(configManager.getProperty("log.level", "INFO")).thenReturn("INFO");
        when(configManager.getInt("server.port", 8080)).thenReturn(8080);
        when(configManager.getProperty("static.folder.path", "")).thenReturn("");
        when(configManager.getProperty("templates.folder.path", "")).thenReturn("");
        when(configManager.getProperty("data.folder.path", "")).thenReturn("");
    }

    @Test
    @DisplayName("Test initial state")
    void testInitialState() {
        assertFalse(processManager.isRunning());
    }

    @Test
    @DisplayName("Test configuration loading")
    void testConfigurationLoading() {
        // Given
        when(configManager.getVersion()).thenReturn("1.0.0");
        
        // When
        processManager.startProcess();
        
        // Then
        verify(configManager, atLeastOnce()).getVersion();
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
        verify(logConsumer, never()).accept(anyString());
    }
} 