package com.github.thkwag.thymelab.launcher.ui.components;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.ui.dialogs.ActuatorInfoDialog;
import com.github.thkwag.thymelab.launcher.util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

public class ControlPanel extends JPanel {
    private JButton startButton;
    private JButton stopButton;
    private JComboBox<String> logLevelCombo;
    private JButton clearLogButton;
    private JLabel logLevelLabel;
    private JTextField logBufferField;
    private JComboBox<String> languageCombo;
    private JLabel logBufferLabel;
    private JLabel languageLabel;
    private JLabel logBufferUnitLabel;
    private JLabel fontSettingsLabel;
    private String selectedFont;
    private int selectedFontSize;
    private JLabel urlLabel;
    private final ConfigManager config;
    private JPanel statusIndicator;
    private Timer blinkTimer;

    // Layout constants
    private static final int BORDER_SPACING = 5;
    private static final int HORIZONTAL_SPACING = 5;
    private static final int VERTICAL_SPACING = 0;
    private static final int TEXT_FIELD_COLUMNS = 5;

    // Status indicator settings
    private static final int STATUS_INDICATOR_SIZE = 12;
    private static final int STATUS_INDICATOR_PADDING = 2;
    private static final int STATUS_INDICATOR_OVAL_PADDING = 4;

    // Color settings
    private static final Color URL_COLOR = new Color(0, 102, 204);
    private static final Color STATUS_ERROR_COLOR = new Color(255, 50, 50);
    private static final Color STATUS_ERROR_BLINK = new Color(180, 0, 0);
    private static final Color STATUS_SUCCESS_COLOR = new Color(0, 180, 0);

    // Timer settings
    private static final int BLINK_INTERVAL = 500;
    private static final int CONNECTION_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 1000;
    private static final int RETRY_INTERVAL = 1000;
    private static final int MAX_RETRY_COUNT = 30;

    // Default settings
    private static final int DEFAULT_PORT = 8080;
    private static final String[] LOG_LEVELS = {"INFO", "DEBUG", "WARN", "ERROR"};
    private static final String[] SUPPORTED_LANGUAGES = {"en", "ko", "ja"};

    // Endpoint paths
    private static final String ENDPOINT_ACTUATOR_HEALTH = "/actuator/health";
    private static final String ENDPOINT_ACTUATOR_LOGGERS = "/actuator/loggers/com.github.thkwag.thymelab";
    private static final String SERVER_URL_FORMAT = "http://localhost:%d";
    private static final String ACTUATOR_URL_FORMAT = SERVER_URL_FORMAT + "%s";

    public ControlPanel(ConfigManager config) {
        this.config = config;
        setLayout(new BorderLayout(BORDER_SPACING, VERTICAL_SPACING));
        setBorder(BorderFactory.createEmptyBorder(BORDER_SPACING, 0, BORDER_SPACING, 0));
        
        initializeComponents();
        layoutComponents();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(BORDER_SPACING, VERTICAL_SPACING));
        setBorder(BorderFactory.createEmptyBorder(BORDER_SPACING, 0, BORDER_SPACING, 0));
        
        // Left panel - Start/Stop buttons and URL
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, HORIZONTAL_SPACING, VERTICAL_SPACING));
        leftPanel.add(startButton);
        leftPanel.add(stopButton);
        leftPanel.add(Box.createHorizontalStrut(HORIZONTAL_SPACING));  // Spacing before URL
        leftPanel.add(Box.createHorizontalStrut(HORIZONTAL_SPACING));
        leftPanel.add(urlLabel);
        leftPanel.add(statusIndicator);
        
        // Right panel - Log level and clear button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, HORIZONTAL_SPACING, VERTICAL_SPACING));
        rightPanel.add(logLevelLabel);
        rightPanel.add(logLevelCombo);
        rightPanel.add(clearLogButton);
        
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private void initializeComponents() {
        startButton = new JButton();
        stopButton = new JButton();
        logLevelCombo = new JComboBox<>();
        clearLogButton = new JButton();
        logLevelLabel = new JLabel();
        logBufferLabel = new JLabel();
        languageLabel = new JLabel();
        logBufferUnitLabel = new JLabel();
        fontSettingsLabel = new JLabel();
        languageCombo = new JComboBox<>();
        logBufferField = new JTextField(TEXT_FIELD_COLUMNS);
        
        logLevelCombo.removeAllItems();
        for (String level : LOG_LEVELS) {
            logLevelCombo.addItem(level);
        }
        
        for (String lang : SUPPORTED_LANGUAGES) {
            Locale locale = Locale.forLanguageTag(lang);
            languageCombo.addItem(locale.getDisplayLanguage(locale).toUpperCase());
        }
        
        // Initialize URL label
        urlLabel = new JLabel();
        urlLabel.setVisible(false);
        urlLabel.setForeground(URL_COLOR);
        urlLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.BOLD));
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(urlLabel.getText()));
                } catch (Exception ex) {
                    AppLogger.error(ex.getMessage(), ex);
                }
            }
        });
        
        logLevelCombo.addActionListener(e -> {
            String level = (String) logLevelCombo.getSelectedItem();
            if (level != null && logLevelCombo.isEnabled()) {
                updateLogLevel(level);
                AppLogger.setLogLevel(level);  // Update local log level
            }
        });
        
        // Start in disabled state
        logLevelCombo.setEnabled(false);
        logLevelLabel.setEnabled(false);
        
        // Initialize status indicator
        statusIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(STATUS_INDICATOR_PADDING, STATUS_INDICATOR_OVAL_PADDING, 
                    getWidth() - STATUS_INDICATOR_PADDING * 2, 
                    getHeight() - STATUS_INDICATOR_PADDING * 2);
            }
        };
        statusIndicator.setPreferredSize(new Dimension(STATUS_INDICATOR_SIZE, STATUS_INDICATOR_SIZE));
        statusIndicator.setOpaque(false);
        statusIndicator.setVisible(false);
        statusIndicator.setCursor(new Cursor(Cursor.HAND_CURSOR));
        statusIndicator.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (statusIndicator.isVisible() && logLevelCombo.isEnabled()) {
                    int port = config.getInt("server.port", DEFAULT_PORT);
                    ActuatorInfoDialog dialog = new ActuatorInfoDialog(
                        (Frame) SwingUtilities.getWindowAncestor(ControlPanel.this), port);
                    dialog.setVisible(true);
                }
            }
        });
        
        // Setup blink timer
        blinkTimer = new Timer(BLINK_INTERVAL, e -> {
            if (statusIndicator.isVisible()) {
                statusIndicator.setBackground(
                    statusIndicator.getBackground().equals(STATUS_ERROR_COLOR) ? 
                    STATUS_ERROR_BLINK : STATUS_ERROR_COLOR
                );
                statusIndicator.repaint();
            }
        });
    }

    private void updateLogLevel(String level) {
        int port = config.getInt("server.port", DEFAULT_PORT);
        URI uri = URI.create(String.format(ACTUATOR_URL_FORMAT, port, ENDPOINT_ACTUATOR_LOGGERS));
        
        new Thread(() -> {
            try {
                AppLogger.debug("Updating log level to: " + level);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    uri.toURL().openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = String.format("{\"configuredLevel\": \"%s\"}", level).getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200 && responseCode != 204) {  // Treat 204 as success too
                    AppLogger.error("Failed to update log level. Server returned: HTTP " + responseCode);
                } else {
                    AppLogger.debug("Log level successfully updated to: " + level);
                }
                
            } catch (Exception ex) {
                AppLogger.error("Failed to update log level: " + ex.getMessage(), ex);
            }
        }).start();
    }

    private void checkActuatorStatus() {
        int port = config.getInt("server.port", DEFAULT_PORT);
        URI uri = URI.create(String.format(ACTUATOR_URL_FORMAT, port, ENDPOINT_ACTUATOR_HEALTH));
        
        new Thread(() -> {
            AppLogger.debug("Starting actuator health check...");
            for (int i = 0; i < MAX_RETRY_COUNT; i++) {  // Try for 30 seconds
                try {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) 
                        uri.toURL().openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(CONNECTION_TIMEOUT);
                    conn.setReadTimeout(READ_TIMEOUT);
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        AppLogger.debug("Actuator health check successful");
                        SwingUtilities.invokeLater(() -> {
                            setLogControlsEnabled(true);
                            statusIndicator.setBackground(STATUS_SUCCESS_COLOR);
                            blinkTimer.stop();
                        });
                        return;
                    }
                    // Response code is not 200
                    AppLogger.debug("Actuator health check failed: HTTP " + responseCode + " (attempt " + (i + 1) + "/" + MAX_RETRY_COUNT + ")");
                    
                } catch (java.net.ConnectException e) {
                    AppLogger.debug("Server not yet ready: " + e.getMessage() + " (attempt " + (i + 1) + "/" + MAX_RETRY_COUNT + ")");
                } catch (Exception e) {
                    AppLogger.error("Error checking actuator status: " + e.getMessage(), e);
                }
                
                try {
                    Thread.sleep(RETRY_INTERVAL);  // Wait 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    AppLogger.debug("Actuator health check interrupted");
                    break;
                }
            }
            // Connection failed after 30 seconds
            AppLogger.error("Actuator health check timed out after " + (MAX_RETRY_COUNT * RETRY_INTERVAL / 1000) + " seconds");
            SwingUtilities.invokeLater(() -> {
                setLogControlsEnabled(false);
                statusIndicator.setBackground(STATUS_ERROR_COLOR);  // Change to red
                blinkTimer.stop();
            });
        }).start();
    }

    public JButton getStartButton() { return startButton; }
    public JButton getStopButton() { return stopButton; }
    public JComboBox<String> getLogLevelCombo() { return logLevelCombo; }
    public JButton getClearLogButton() { return clearLogButton; }
    public JTextField getLogBufferField() {
        return logBufferField;
    }
    public JComboBox<String> getLanguageCombo() {
        return languageCombo;
    }
    public JLabel getLogLevelLabel() { return logLevelLabel; }
    public JLabel getLogBufferLabel() { return logBufferLabel; }
    public JLabel getLanguageLabel() { return languageLabel; }
    public JLabel getLogBufferUnit() { return logBufferUnitLabel; }
    public JLabel getFontSettingsText() { return fontSettingsLabel; }

    public void setSelectedFont(String font) {
        this.selectedFont = font;
    }

    public void setSelectedFontSize(int size) {
        this.selectedFontSize = size;
    }

    public String getSelectedFont() {
        return selectedFont;
    }

    public int getSelectedFontSize() {
        return selectedFontSize;
    }

    public void setFontSettingsText(String fontText, String sizeText) {
        fontSettingsLabel.setText(fontText + " / " + sizeText);
    }

    public void setLogControlsEnabled(boolean enabled) {
        logLevelCombo.setEnabled(enabled);
        logLevelLabel.setEnabled(enabled);
        if (!enabled) {
            logLevelCombo.setToolTipText("Actuator is not available");
        } else {
            logLevelCombo.setToolTipText(null);
        }
    }

    public void onProcessStarted() {
        AppLogger.debug("Application process started");
        setLogControlsEnabled(false);  // Initially disabled, will be enabled by checkActuatorStatus
        urlLabel.setVisible(true);
        statusIndicator.setVisible(true);
        statusIndicator.setBackground(new Color(255, 50, 50));
        blinkTimer.start();
        checkActuatorStatus();
    }

    public void onProcessStarted(String serverUrl) {
        AppLogger.debug("Application process started at: " + serverUrl);
        urlLabel.setText(serverUrl);
        onProcessStarted();
    }

    public void onProcessStopped() {
        AppLogger.debug("Application process stopped");
        setLogControlsEnabled(false);
        urlLabel.setVisible(false);
        statusIndicator.setVisible(false);
        blinkTimer.stop();
    }

    public void updateTexts(ResourceBundle bundle) {
        startButton.setText(bundle.getString("start"));
        stopButton.setText(bundle.getString("stop"));
        clearLogButton.setText(bundle.getString("clear_log"));
        logLevelLabel.setText(bundle.getString("log_level"));
        logBufferLabel.setText(bundle.getString("log_buffer_size"));
        languageLabel.setText(bundle.getString("language"));
        logBufferUnitLabel.setText(bundle.getString("lines"));
        fontSettingsLabel.setText(bundle.getString("font") + " / " + bundle.getString("font_size"));
    }
} 