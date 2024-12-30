package com.github.thkwag.thymelab.launcher.ui;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.config.LocaleManager;
import com.github.thkwag.thymelab.launcher.process.AppProcessManager;
import com.github.thkwag.thymelab.launcher.ui.components.ControlPanel;
import com.github.thkwag.thymelab.launcher.ui.components.LogPanel;
import com.github.thkwag.thymelab.launcher.ui.components.MainMenuBar;
import com.github.thkwag.thymelab.launcher.ui.dialogs.AboutDialog;
import com.github.thkwag.thymelab.launcher.util.AppLogger;
import com.github.thkwag.thymelab.launcher.config.ConfigManager.LanguageChangeListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainFrame extends JFrame implements LanguageChangeListener {
    private final ConfigManager config;
    private final LocaleManager localeManager;
    private ResourceBundle bundle;

    private AppProcessManager appProcessManager;
    private final MainForm mainForm;
    private ControlPanel controlPanel;
    private final MainMenuBar menuBar;

    private TrayIcon trayIcon;
    private MenuItem showHideMenuItem;
    private MenuItem exitMenuItem;
    private MenuItem serverStatusItem;
    private boolean isRunning = false;

    // Default settings
    private static final String DEFAULT_LANGUAGE = "en";
    private static final int DEFAULT_BUFFER_SIZE = 1000;
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int DEFAULT_PORT = 8080;

    // Window settings
    private static final int DEFAULT_WINDOW_WIDTH = 1024;
    private static final int DEFAULT_WINDOW_HEIGHT = 768;
    private static final int DEFAULT_WINDOW_X = 200;
    private static final int DEFAULT_WINDOW_Y = 150;
    private static final int DEFAULT_WINDOW_STATE = Frame.NORMAL;

    // Icon settings
    private static final String[] ICON_PATHS = {
        "/icons/icon.png",
        "/icons/icon-16.png",
        "/icons/icon-32.png",
        "/icons/icon-64.png"
    };
    private static final String MAC_ICON_PATH = "/icons/icon.icns";
    private static final String WINDOWS_ICON_PATH = "/icons/icon.ico";
    private static final String DEFAULT_ICON_PATH = "/icons/icon.png";

    // Tray icon settings
    private static final int DEFAULT_TRAY_ICON_SIZE = 16;
    private static final Color DEFAULT_TRAY_ICON_COLOR = Color.GREEN;
    private static final Color DEFAULT_TRAY_ICON_BORDER = Color.BLACK;

    public MainFrame(ConfigManager config, LocaleManager localeManager) {
        this.config = config;
        this.localeManager = localeManager;
        this.bundle = localeManager.getBundle();

        config.addLanguageChangeListener(this);

        mainForm = new MainForm(config);
        menuBar = mainForm.getMainMenuBar();
        setJMenuBar(menuBar);
        setContentPane(mainForm.getMainPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setupIcon();
        setupWindowListeners();
        updateTitle();
        initActions();
        loadSettings();
        setupProcessManager();
        setupTrayIcon();

        // Start process after window is shown
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> startApp());
            }
        });
    }

    @Override
    public void onLanguageChange(String languageCode) {
        this.bundle = localeManager.getBundle();
        updateTexts();
    }

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    SystemTray.getSystemTray().remove(trayIcon);
                }
                saveWindowState();
                System.exit(0);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        setVisible(false);
                        setState(Frame.NORMAL);
                    } else {
                        setVisible(false);
                    }
                }
            }
        });

        // macOS specific listeners
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            addWindowStateListener(e -> {
                if ((e.getNewState() & Frame.ICONIFIED) != 0) {
                    setVisible(false);
                    setState(Frame.NORMAL);
                }
            });
        }
    }

    private void loadSettings() {
        // Load buffer size
        updateBufferSize();

        // Load log level
        String savedLevel = config.getProperty("log.level", DEFAULT_LOG_LEVEL);
        mainForm.setLogLevelComboItem(savedLevel);

        // Load language
        updateLanguageCombo();

        // Load font settings
        updateFontSettings();

        // Initialize UI state
        updateButtonStates(true);
        updateTexts();
    }

    private void setupProcessManager() {
        appProcessManager = new AppProcessManager(
            mainForm::appendLog,
            () -> SwingUtilities.invokeLater(() -> updateButtonStates(false)),
            config
        );
    }

    private void updateTitle() {
        String title = String.format("%s - %s", 
            bundle.getString("app_title"), 
            bundle.getString("sub_title"));
        setTitle(title);
    }

    private void updateLanguageCombo() {
        String langCode = getLanguageCode();
        Locale locale = Locale.forLanguageTag(langCode);
        String displayLanguage = locale.getDisplayLanguage(locale);
        mainForm.setLanguageComboItem(displayLanguage);
    }

    private String getLanguageCode() {
        return config.getProperty("language", DEFAULT_LANGUAGE);
    }

    private void updateBufferSize() {
        int bufSize = config.getInt("log.buffer.size", DEFAULT_BUFFER_SIZE);
        mainForm.setMaxBufferSize(bufSize);
        mainForm.setLogBufferText(String.valueOf(bufSize));
    }

    private void updateFontSettings() {
        String fontFamily = config.getProperty("font.family", LogPanel.getDefaultMonospacedFont());
        int fontSize = config.getInt("font.size", DEFAULT_FONT_SIZE);
        mainForm.updateLogFont(fontFamily, fontSize);
    }

    private void initActions() {
        controlPanel = mainForm.getControlPanel();
        setupButtonActions();
        setupMenuActions();
        setupLanguageComboAction();
    }

    private void setupButtonActions() {
        controlPanel.getStartButton().addActionListener(e -> startApp());
        controlPanel.getStopButton().addActionListener(e -> stopApp());
        controlPanel.getClearLogButton().addActionListener(e -> mainForm.clearLog());

        controlPanel.getLogLevelCombo().addActionListener(e -> {
            String level = (String) controlPanel.getLogLevelCombo().getSelectedItem();
            if (level != null) {
                config.setProperty("log.level", level);
            }
        });
    }

    private void setupMenuActions() {
        mainForm.getProgramSettingsMenuItem().addActionListener(e -> {
            mainForm.showSettingsDialog(this);
            updateFromSettings();
        });

        mainForm.getThymeleafSettingsMenuItem().addActionListener(e -> 
            mainForm.showThymeleafSettingsDialog(this, bundle, config));

        mainForm.getAboutMenuItem().addActionListener(e -> showAboutDialog(this));
    }

    private void setupLanguageComboAction() {
        controlPanel.getLanguageCombo().addActionListener(e -> {
            String lang = (String) controlPanel.getLanguageCombo().getSelectedItem();
            if (lang != null) {
                String code = Locale.forLanguageTag(lang).toLanguageTag().toLowerCase();
                config.setProperty("language", code);
                updateLanguage();
            }
        });
    }

    private void updateLanguage() {
        String langCode = getLanguageCode();
        if (!langCode.equals(localeManager.getCurrentLanguage())) {
            localeManager.setLanguage(langCode);
            bundle = localeManager.getBundle();
            updateTexts();
        }
    }

    public void updateFromSettings() {
        updateLanguage();
        updateBufferSize();
        updateFontSettings();
    }

    public void updateTexts() {
        updateButtons();
        updateLabels();
        updateMenus();
        updateTitle();
        updateTrayIcon();
    }

    private void updateButtons() {
        controlPanel.getStartButton().setText(bundle.getString("start"));
        controlPanel.getStopButton().setText(bundle.getString("stop"));
        controlPanel.getClearLogButton().setText(bundle.getString("clear_log"));
    }

    private void updateLabels() {
        controlPanel.getLogLevelLabel().setText(bundle.getString("log_level"));
        controlPanel.getLogBufferLabel().setText(bundle.getString("log_buffer_size"));
        controlPanel.getLanguageLabel().setText(bundle.getString("language"));
        controlPanel.getLogBufferUnit().setText(bundle.getString("lines"));
        controlPanel.setFontSettingsText(bundle.getString("font"), bundle.getString("font_size"));
    }

    private void updateMenus() {
        menuBar.getToolsMenu().setText(bundle.getString("menu_tools"));
        menuBar.getHelpMenu().setText(bundle.getString("menu_help"));
        menuBar.getProgramSettingsMenuItem().setText(bundle.getString("menu_program_settings"));
        menuBar.getThymeleafSettingsMenuItem().setText(bundle.getString("menu_thymeleaf_settings"));
        menuBar.getExitMenuItem().setText(bundle.getString("menu_exit"));
        menuBar.getAboutMenuItem().setText(bundle.getString("menu_about"));
    }

    private void startApp() {
        if (!appProcessManager.isRunning()) {
            appProcessManager.startProcess();
            updateButtonStates(true);
            onProcessStart();
        }
    }

    private void stopApp() {
        if (appProcessManager.isRunning()) {
            appProcessManager.stopProcess();
            updateButtonStates(false);
            onProcessStop();
        }
    }

    private void updateButtonStates(boolean isRunning) {
        controlPanel.getStartButton().setEnabled(!isRunning);
        controlPanel.getStopButton().setEnabled(isRunning);
    }

    public void loadWindowState() {
        int width = config.getInt("window.width", DEFAULT_WINDOW_WIDTH);
        int height = config.getInt("window.height", DEFAULT_WINDOW_HEIGHT);
        int x = config.getInt("window.x", DEFAULT_WINDOW_X);
        int y = config.getInt("window.y", DEFAULT_WINDOW_Y);
        int state = config.getInt("window.state", DEFAULT_WINDOW_STATE);

        // Set screen size
        setSize(width, height);
        
        // Check all screen areas
        Rectangle virtualBounds = new Rectangle();
        for (GraphicsDevice screen : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            virtualBounds = virtualBounds.union(screen.getDefaultConfiguration().getBounds());
        }

        // Verify if saved position is valid
        if (!virtualBounds.contains(x, y)) {
            // Position at screen center
            x = (int) (virtualBounds.getCenterX() - (double) width / 2);
            y = (int) (virtualBounds.getCenterY() - (double) height / 2);
        }

        setLocation(x, y);
        setExtendedState(state);
    }

    public void saveWindowState() {
        config.setInt("window.width", getWidth());
        config.setInt("window.height", getHeight());
        config.setInt("window.x", getX());
        config.setInt("window.y", getY());
        config.setInt("window.state", getExtendedState());
    }

    public void startProcess() {
        mainForm.startProcess();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (showHideMenuItem != null) {
            showHideMenuItem.setLabel(visible ? bundle.getString("hide") : bundle.getString("show"));
        }
        if (visible) {
            toFront();
        }
    }

    private void onProcessStart() {
        isRunning = true;
        updateTrayIcon();
        mainForm.getControlPanel().onProcessStarted(getServerUrl());
    }

    private void onProcessStop() {
        isRunning = false;
        updateTrayIcon();
        mainForm.getControlPanel().onProcessStopped();
    }

    private void setupTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu popup = new PopupMenu();

            // Show/Hide menu item
            showHideMenuItem = new MenuItem(bundle.getString("show"));
            showHideMenuItem.addActionListener(e -> toggleVisibility());
            popup.add(showHideMenuItem);

            // Server status menu item (clickable when server is running)
            serverStatusItem = new MenuItem(getStatusText());
            serverStatusItem.addActionListener(e -> {
                if (isRunning) {
                    try {
                        Desktop.getDesktop().browse(new java.net.URI(getServerUrl()));
                    } catch (Exception ex) {
                        AppLogger.error("Could not open browser: " + ex.getMessage());
                    }
                }
            });
            serverStatusItem.setEnabled(false);  // Initially disabled
            popup.add(serverStatusItem);

            // Add separator before exit
            popup.addSeparator();

            // Exit menu item
            exitMenuItem = new MenuItem(bundle.getString("menu_exit"));
            exitMenuItem.addActionListener(e -> {
                if (SystemTray.isSupported()) {
                    tray.remove(trayIcon);
                }
                saveWindowState();
                System.exit(0);
            });
            popup.add(exitMenuItem);

            try {
                // Try multiple icon paths
                Image image = null;
                
                for (String path : ICON_PATHS) {
                    try {
                        image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path)));
                        if (image != null) break;
                    } catch (Exception ignored) {}
                }
                
                // If no icon found, create a default one
                if (image == null) {
                    image = createDefaultIcon();
                }
                
                trayIcon = new TrayIcon(image, getTooltipText(), popup);
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(e -> toggleVisibility());
                
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    AppLogger.error("Could not add system tray icon: " + e.getMessage());
                }
            } catch (Exception e) {
                AppLogger.error("Could not setup system tray icon: " + e.getMessage());
            }
        }
    }

    private Image createDefaultIcon() {
        // Create a simple default icon
        BufferedImage image = new BufferedImage(DEFAULT_TRAY_ICON_SIZE, DEFAULT_TRAY_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(DEFAULT_TRAY_ICON_COLOR);
        g2d.fillRect(0, 0, DEFAULT_TRAY_ICON_SIZE, DEFAULT_TRAY_ICON_SIZE);
        g2d.setColor(DEFAULT_TRAY_ICON_BORDER);
        g2d.drawRect(0, 0, DEFAULT_TRAY_ICON_SIZE - 1, DEFAULT_TRAY_ICON_SIZE - 1);
        g2d.dispose();
        return image;
    }

    private void toggleVisibility() {
        boolean currentlyVisible = isVisible();
        setVisible(!currentlyVisible);
        if (!currentlyVisible) {
            setState(Frame.NORMAL);
            requestFocus();
            toFront();
        }
    }

    private String getStatusText() {
        if (isRunning) {
            return String.format("%s - %s", getServerUrl(), bundle.getString("running"));
        }
        return bundle.getString("stopped");
    }

    private String getTooltipText() {
        return String.format("%s - %s", 
            bundle.getString("app_title"), 
            getStatusText());
    }

    private void updateTrayIcon() {
        if (trayIcon != null) {
            // Update tooltip with current status
            trayIcon.setToolTip(getTooltipText());
            
            // Update menu items
            if (showHideMenuItem != null) {
                showHideMenuItem.setLabel(isVisible() ? bundle.getString("hide") : bundle.getString("show"));
            }
            if (exitMenuItem != null) {
                exitMenuItem.setLabel(bundle.getString("menu_exit"));
            }
            if (serverStatusItem != null) {
                serverStatusItem.setEnabled(isRunning);
                serverStatusItem.setLabel(getStatusText());
            }
        }
    }

    private void setupIcon() {
        try {
            // Set platform-specific icon
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                setIconImage(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(MAC_ICON_PATH))));
            } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                setIconImage(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(WINDOWS_ICON_PATH))));
            } else {
                setIconImage(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_ICON_PATH))));
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void showAboutDialog(MainFrame parent) {
        AboutDialog dialog = new AboutDialog(parent, bundle, config);
        dialog.setVisible(true);
    }

    private String getServerUrl() {
        int port = config.getInt("server.port", DEFAULT_PORT);
        return String.format("http://localhost:%d", port);
    }
} 