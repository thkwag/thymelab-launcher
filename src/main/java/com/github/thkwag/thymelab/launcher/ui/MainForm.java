package com.github.thkwag.thymelab.launcher.ui;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.process.AppProcessManager;
import com.github.thkwag.thymelab.launcher.ui.components.ControlPanel;
import com.github.thkwag.thymelab.launcher.ui.components.LogPanel;
import com.github.thkwag.thymelab.launcher.ui.components.MainMenuBar;
import com.github.thkwag.thymelab.launcher.ui.dialogs.AboutDialog;
import com.github.thkwag.thymelab.launcher.ui.dialogs.SettingsDialog;
import com.github.thkwag.thymelab.launcher.ui.dialogs.ThymeleafSettingsDialog;
import com.github.thkwag.thymelab.launcher.config.ConfigManager.LanguageChangeListener;
import com.github.thkwag.thymelab.launcher.util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

public class MainForm extends JFrame implements LanguageChangeListener {
    private final JPanel mainPanel;
    private final LogPanel logPanel;
    private final ControlPanel controlPanel;
    private final MainMenuBar menuBar;
    private final ConfigManager config;
    private AppProcessManager processManager;

    // Layout constants
    private static final int BORDER_SPACING = 0;
    private static final int VERTICAL_SPACING = 1;
    private static final int PANEL_PADDING = 5;

    public MainForm(ConfigManager config) {
        this.config = config;
        config.addLanguageChangeListener(this);
        
        mainPanel = new JPanel(new BorderLayout(BORDER_SPACING, VERTICAL_SPACING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));
        
        logPanel = new LogPanel();
        controlPanel = new ControlPanel(config);
        ResourceBundle bundle = config.getLocaleManager().getBundle();
        menuBar = new MainMenuBar(this, bundle);
        
        layoutComponents();
        initializeListeners();

        addShutdownHook();
        setUncaughtExceptionHandler();

        // Initialize UI components
        initComponents();
        
        // Add WindowListener to initialize functionality after window is fully displayed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    initializeFunctionality();
                });
            }
        });
    }

    private void layoutComponents() {
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(logPanel, BorderLayout.CENTER);
    }

    private void initializeListeners() {
        // ... Initialize listeners
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // process.destroy();
        }));
    }

    private void setUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppLogger.error("Unhandled exception caught: " + throwable.getMessage(), throwable);
            // Exit program on exception
            System.exit(1);
        });
    }

    public MainMenuBar getMainMenuBar() {
        return menuBar;
    }

    public void startProcess() {
        if (processManager != null) {
            processManager.startProcess();
        }
    }

    public void stopProcess() {
        if (processManager != null) {
            processManager.stopProcess();
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void appendLog(String line) {
        logPanel.appendLog(line);
    }

    public void setMaxBufferSize(int size) {
        logPanel.setMaxBufferSize(size);
    }

    public void setLogBufferText(String text) {
        controlPanel.getLogBufferField().setText(text);
    }

    public void setLogLevelComboItem(String item) {
        controlPanel.getLogLevelCombo().setSelectedItem(item);
    }

    public void setLanguageComboItem(String item) {
        controlPanel.getLanguageCombo().setSelectedItem(item);
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public JMenuItem getProgramSettingsMenuItem() {
        return menuBar.getProgramSettingsMenuItem();
    }

    public JMenuItem getThymeleafSettingsMenuItem() {
        return menuBar.getThymeleafSettingsMenuItem();
    }

    public JMenuItem getAboutMenuItem() {
        return menuBar.getAboutMenuItem();
    }

    public void clearLog() {
        logPanel.clearLog();
    }

    public void setSelectedFont(String font) {
        controlPanel.setSelectedFont(font);
    }

    public void setSelectedFontSize(int size) {
        controlPanel.setSelectedFontSize(size);
    }

    public String getSelectedFont() {
        return controlPanel.getSelectedFont();
    }

    public int getSelectedFontSize() {
        return controlPanel.getSelectedFontSize();
    }

    public void updateLogFont(String fontFamily, int fontSize) {
        logPanel.updateLogFont(fontFamily, fontSize);
    }

    public void showSettingsDialog(MainFrame parent) {
        SettingsDialog dialog = new SettingsDialog(parent, config, config.getLocaleManager().getBundle());
        dialog.setVisible(true);
    }

    public void showThymeleafSettingsDialog(Frame parent, ResourceBundle bundle, ConfigManager config) {
        ThymeleafSettingsDialog dialog = new ThymeleafSettingsDialog(parent, config.getLocaleManager().getBundle(), config);
        dialog.setVisible(true);
    }

    public void showAboutDialog(MainFrame parent) {
        AboutDialog dialog = new AboutDialog(parent, config.getLocaleManager().getBundle(), config);
        dialog.setVisible(true);
    }

    private void initComponents() {
    }

    private void initializeFunctionality() {
        processManager = new AppProcessManager(
                logPanel::appendLog,
                controlPanel::onProcessStopped,
            config
        );

        // Set button event handlers
        controlPanel.getStartButton().addActionListener(e -> startProcess());
        controlPanel.getStopButton().addActionListener(e -> stopProcess());
    
    }

    @Override
    public void onLanguageChange(String languageCode) {
        ResourceBundle bundle = config.getLocaleManager().getBundle();
        menuBar.updateTexts(bundle);
        controlPanel.updateTexts(bundle);
    }
} 