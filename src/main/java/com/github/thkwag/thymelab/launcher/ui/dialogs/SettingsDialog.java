package com.github.thkwag.thymelab.launcher.ui.dialogs;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.ui.MainFrame;
import com.github.thkwag.thymelab.launcher.ui.components.LogPanel;
import com.github.thkwag.thymelab.launcher.util.AppLogger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.*;
import java.io.File;
import org.kohsuke.github.*;
import okhttp3.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SettingsDialog extends JDialog {
    private JComboBox<LanguageItem> languageCombo;
    private JComboBox<String> fontCombo;
    private JSpinner fontSizeSpinner;
    private JTextField logBufferField;
    private JTextField jarPathField;
    private final ConfigManager config;
    private ResourceBundle bundle;
    private static final String[] LABEL_KEYS = {"language", "font", "log_buffer_size", "font_size", "port", "lines", "processor_jar_path"};
    private JLabel[] labels;
    private JButton saveButton;
    private JSpinner portSpinner;
    private JLabel logBufferUnitLabel;
    private JButton selectJarButton;

    // Port settings
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    private static final int DEFAULT_PORT = 8080;

    // Font settings
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 72;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int FONT_SIZE_STEP = 1;

    // Component dimensions
    private static final int LABEL_WIDTH = 150;
    private static final int FIELD_WIDTH = 100;
    private static final int PANEL_WIDTH = 150;
    private static final int COMBO_WIDTH = 250;
    private static final int TEXT_FIELD_COLUMNS = 5;
    private static final int DIALOG_WIDTH = 600;

    // Default values
    private static final int DEFAULT_BUFFER_SIZE = 1000;
    private static final String DEFAULT_LANGUAGE = "en";

    // Layout constants
    private static final int BORDER_PADDING = 10;
    private static final int BORDER_BOTTOM = 5;
    private static final int COMPONENT_SPACING = 5;
    private static final int LABEL_CONTROL_GAP = 2;
    private static final int FLOW_HGAP = 2;
    private static final int FLOW_VGAP = 0;

    private static final String GITHUB_REPO = "thkwag/thymelab";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private record LanguageItem(String code, String displayName) {

        @Override
            public String toString() {
                return displayName;
            }
        }

    public SettingsDialog(Frame parent, ConfigManager config, ResourceBundle bundle) {
        super(parent, "Settings", true);
        this.config = config;
        this.bundle = bundle;
        
        setTitle(bundle.getString("menu_program_settings"));
        setResizable(true);
        
        initializeComponents();
        layoutComponents();
        initializeValues();
        
        // Set minimum size for dialog
        setMinimumSize(new Dimension(DIALOG_WIDTH, getPreferredSize().height));
    }

    private void initializeComponents() {
        fontCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_FONT_SIZE, MIN_FONT_SIZE, MAX_FONT_SIZE, FONT_SIZE_STEP));
        
        // Initialize language combo with supported languages
        languageCombo = new JComboBox<>();
        for (String code : config.getLocaleManager().getSupportedLanguages()) {
            try {
                ResourceBundle langBundle = ResourceBundle.getBundle("i18n/messages", 
                    Locale.forLanguageTag(code), this.getClass().getClassLoader());
                String displayName = langBundle.getString("language_self");
                languageCombo.addItem(new LanguageItem(code, displayName));
            } catch (MissingResourceException e) {
                AppLogger.warn("Failed to load language name for " + code + ": " + e.getMessage());
                continue;
            }
        }

        logBufferField = new JTextField(TEXT_FIELD_COLUMNS);
        portSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_PORT, MIN_PORT, MAX_PORT, 1));
        JSpinner.NumberEditor portEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portEditor);
        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setColumns(TEXT_FIELD_COLUMNS);

        // Add JAR path field
        jarPathField = new JTextField(config.getProcessorJarPath(), TEXT_FIELD_COLUMNS);
        selectJarButton = new JButton(bundle.getString("select_jar"));
        
        selectJarButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                jarPathField.setText(path);
            }
        });

        // Add listener for immediate application
        fontCombo.addActionListener(e -> {
            String selectedFont = (String) fontCombo.getSelectedItem();
            int selectedSize = (Integer) fontSizeSpinner.getValue();
            config.setProperty("font.family", selectedFont);
            config.setInt("font.size", selectedSize);
            updateParentUI();
        });

        fontSizeSpinner.addChangeListener(e -> {
            String selectedFont = (String) fontCombo.getSelectedItem();
            int selectedSize = (Integer) fontSizeSpinner.getValue();
            config.setProperty("font.family", selectedFont);
            config.setInt("font.size", selectedSize);
            updateParentUI();
        });

        languageCombo.addActionListener(e -> {
            LanguageItem selectedItem = (LanguageItem) languageCombo.getSelectedItem();
            if (selectedItem != null) {
                config.changeLanguage(selectedItem.code);
                // Update dialog language
                this.bundle = ResourceBundle.getBundle("i18n/messages", 
                    Locale.forLanguageTag(selectedItem.code), this.getClass().getClassLoader());
                updateTexts();
                // Update main window
                updateParentUI();
            }
        });

        logBufferField.addActionListener(e -> {
            try {
                int bufferSize = Integer.parseInt(logBufferField.getText().trim());
                if (bufferSize > 0) {
                    config.setInt("log.buffer.size", bufferSize);
                    updateParentUI();
                }
            } catch (NumberFormatException ex) {
                // Ignore invalid input
            }
        });

        portSpinner.addChangeListener(e -> {
            int port = (Integer) portSpinner.getValue();
            config.setInt("server.port", port);
            updateParentUI();
        });
    }

    private void updateParentUI() {
        if (getParent() instanceof MainFrame parent) {
            parent.updateFromSettings();
        }
    }

    private void layoutComponents() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_BOTTOM, BORDER_PADDING));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING, LABEL_CONTROL_GAP);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        
        // Initialize labels (fixed width)
        this.labels = new JLabel[LABEL_KEYS.length];
        for (int i = 0; i < LABEL_KEYS.length; i++) {
            labels[i] = new JLabel(bundle.getString(LABEL_KEYS[i]) + ":");
            labels[i].setPreferredSize(new Dimension(LABEL_WIDTH, labels[i].getPreferredSize().height));
            labels[i].setMinimumSize(new Dimension(LABEL_WIDTH, labels[i].getPreferredSize().height));
            labels[i].setHorizontalAlignment(SwingConstants.RIGHT);
        }
        
        // Language
        gbc.gridy = 0;
        panel.add(labels[0], gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        languageCombo.setPreferredSize(new Dimension(COMBO_WIDTH, languageCombo.getPreferredSize().height));
        panel.add(languageCombo, gbc);
        
        // Buffer size
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(labels[2], gbc);
        
        JPanel bufferPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, FLOW_HGAP, FLOW_VGAP));
        bufferPanel.setPreferredSize(new Dimension(PANEL_WIDTH, logBufferField.getPreferredSize().height));
        logBufferField.setPreferredSize(new Dimension(FIELD_WIDTH, logBufferField.getPreferredSize().height));
        bufferPanel.add(logBufferField);
        logBufferUnitLabel = new JLabel(bundle.getString("lines"));
        bufferPanel.add(logBufferUnitLabel);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(bufferPanel, gbc);
        
        // Font settings
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(labels[1], gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        fontCombo.setPreferredSize(new Dimension(COMBO_WIDTH, fontCombo.getPreferredSize().height));
        panel.add(fontCombo, gbc);
        
        // Font size
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(labels[3], gbc);
        
        JPanel fontSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, FLOW_HGAP, FLOW_VGAP));
        fontSizePanel.setPreferredSize(new Dimension(PANEL_WIDTH, fontSizeSpinner.getPreferredSize().height));
        fontSizeSpinner.setPreferredSize(new Dimension(FIELD_WIDTH, fontSizeSpinner.getPreferredSize().height));
        fontSizePanel.add(fontSizeSpinner);
        fontSizePanel.add(new JLabel("pt"));
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(fontSizePanel, gbc);
        
        // Port
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(labels[4], gbc);
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, FLOW_HGAP, FLOW_VGAP));
        portPanel.setPreferredSize(new Dimension(PANEL_WIDTH, portSpinner.getPreferredSize().height));
        portSpinner.setPreferredSize(new Dimension(FIELD_WIDTH, portSpinner.getPreferredSize().height));
        portPanel.add(portSpinner);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(portPanel, gbc);
        
        // JAR path
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(labels[6], gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton downloadButton = new JButton(bundle.getString("download"));
        downloadButton.addActionListener(e -> showReleaseDialog());
        downloadPanel.add(downloadButton);
        downloadPanel.add(new JLabel(bundle.getString("download_from_repo")));
        panel.add(downloadPanel, gbc);

        // JAR path field with select button (new row)
        gbc.gridy = 6;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel jarPathPanel = new JPanel(new BorderLayout(5, 0));
        jarPathPanel.add(jarPathField, BorderLayout.CENTER);
        jarPathPanel.add(selectJarButton, BorderLayout.EAST);
        panel.add(jarPathPanel, gbc);
        gbc.gridwidth = 1;

        // Save button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(COMPONENT_SPACING, 0, COMPONENT_SPACING, 0));
        this.saveButton = new JButton(bundle.getString("save"));
        saveButton.addActionListener(e -> {
            saveSettings();
            setVisible(false);
        });
        buttonPanel.add(saveButton);
        
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void initializeValues() {
        // Load current settings
        String currentFont = config.getProperty("font.family", LogPanel.getDefaultMonospacedFont());
        int currentSize = config.getInt("font.size", DEFAULT_FONT_SIZE);
        String currentLang = config.getProperty("language", DEFAULT_LANGUAGE);
        int currentBuffer = config.getInt("log.buffer.size", DEFAULT_BUFFER_SIZE);
        int currentPort = config.getInt("server.port", DEFAULT_PORT);

        // Set current values to controls
        fontCombo.setSelectedItem(currentFont);
        fontSizeSpinner.setValue(currentSize);
        
        // Set language
        for (int i = 0; i < languageCombo.getItemCount(); i++) {
            LanguageItem item = languageCombo.getItemAt(i);
            if (item.code.equals(currentLang)) {
                languageCombo.setSelectedIndex(i);
                break;
            }
        }
        
        logBufferField.setText(String.valueOf(currentBuffer));
        portSpinner.setValue(currentPort);
        
        // Set JAR path
        jarPathField.setText(config.getProcessorJarPath());
    }

    private void saveSettings() {
        // Save font settings
        String selectedFont = (String) fontCombo.getSelectedItem();
        int selectedSize = (Integer) fontSizeSpinner.getValue();
        config.setProperty("font.family", selectedFont);
        config.setInt("font.size", selectedSize);

        // Save language settings
        LanguageItem selectedItem = (LanguageItem) languageCombo.getSelectedItem();
        if (selectedItem != null) {
            config.setProperty("language", selectedItem.code);
        }

        // Save log buffer size
        try {
            int bufferSize = Integer.parseInt(logBufferField.getText().trim());
            if (bufferSize > 0) {
                config.setInt("log.buffer.size", bufferSize);
            }
        } catch (NumberFormatException ex) {
            // Ignore invalid input
        }

        // Save port setting
        int port = (Integer) portSpinner.getValue();
        if (port >= MIN_PORT && port <= MAX_PORT) {
            config.setInt("server.port", port);
        }

        // Save JAR path
        config.setProcessorJarPath(jarPathField.getText().trim());
    }

    private void updateTexts() {
        setTitle(bundle.getString("settings"));
        
        // Update labels
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(bundle.getString(LABEL_KEYS[i]) + ":");
        }
        
        // Update unit label
        logBufferUnitLabel.setText(bundle.getString("lines"));
        
        // Update buttons
        saveButton.setText(bundle.getString("save"));
        selectJarButton.setText(bundle.getString("select_jar"));

        // Update download panel if exists
        for (Component comp : ((JPanel)getContentPane().getComponent(0)).getComponents()) {
            if (comp instanceof JPanel) {
                for (Component innerComp : ((JPanel)comp).getComponents()) {
                    if (innerComp instanceof JPanel && ((JPanel)innerComp).getLayout() instanceof FlowLayout) {
                        JPanel panel = (JPanel)innerComp;
                        for (Component c : panel.getComponents()) {
                            if (c instanceof JButton && ((JButton)c).getActionListeners().length > 0) {
                                ((JButton)c).setText(bundle.getString("download"));
                            } else if (c instanceof JLabel) {
                                ((JLabel)c).setText(bundle.getString("download_from_repo"));
                            }
                        }
                    }
                }
            }
        }
        
        pack();
    }

    private void showReleaseDialog() {
        try {
            GitHub github = GitHub.connectAnonymously();
            GHRepository repo = github.getRepository(GITHUB_REPO);
            List<GHRelease> releases = repo.listReleases().toList();
            
            if (releases.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    bundle.getString("no_releases_found"), 
                    bundle.getString("error"), 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create release selection dialog
            JDialog dialog = new JDialog(this, bundle.getString("select_release"), true);
            dialog.setLayout(new BorderLayout());
            
            // Create release list
            DefaultListModel<GHRelease> listModel = new DefaultListModel<>();
            releases.forEach(listModel::addElement);
            
            JList<GHRelease> releaseList = new JList<>(listModel);
            releaseList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, 
                        int index, boolean isSelected, boolean cellHasFocus) {
                    GHRelease release = (GHRelease) value;
                    String label = String.format("%s (%s)", 
                        release.getName(), 
                        release.getTagName());
                    return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                }
            });
            
            JScrollPane scrollPane = new JScrollPane(releaseList);
            dialog.add(scrollPane, BorderLayout.CENTER);
            
            // Add selection button
            JButton selectButton = new JButton(bundle.getString("download"));
            selectButton.addActionListener(e -> {
                GHRelease selected = releaseList.getSelectedValue();
                if (selected != null) {
                    dialog.dispose();
                    downloadRelease(selected);
                }
            });
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(selectButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            // Show dialog
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                String.format(bundle.getString("failed_fetch_releases"), e.getMessage()), 
                bundle.getString("error"), 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void downloadRelease(GHRelease release) {
        try {
            // Find processor JAR asset
            String version = release.getTagName().replace("v", "");
            String jarPattern = String.format("thymelab-processor-%s.jar", version);
            GHAsset jarAsset = release.listAssets().toList().stream()
                .filter(asset -> {
                    String name = asset.getName().toLowerCase();
                    return name.contains("processor") && name.endsWith(".jar");
                })
                .findFirst()
                .orElseThrow(() -> new IOException(bundle.getString("processor_jar_not_found")));

            // Create progress dialog
            JDialog progressDialog = new JDialog(this, bundle.getString("downloading"), true);
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString(bundle.getString("preparing_download"));
            progressBar.setPreferredSize(new Dimension(400, 25));
            
            JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
            progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            progressPanel.add(progressBar, BorderLayout.CENTER);
            
            progressDialog.add(progressPanel);
            progressDialog.setMinimumSize(new Dimension(400, 100));
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(this);
            
            // Start download in background
            CompletableFuture.runAsync(() -> {
                try {
                    // Create download request
                    Request request = new Request.Builder()
                        .url(jarAsset.getBrowserDownloadUrl())
                        .build();

                    // Execute request
                    Response response = HTTP_CLIENT.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException("Download failed: " + response.code());
                    }

                    // Prepare download location
                    File downloadDir = new File(System.getProperty("user.home"), ".thymelab");
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }
                    
                    File outputFile = new File(downloadDir, jarPattern);
                    long totalBytes = response.body().contentLength();
                    
                    // Download file
                    try (InputStream in = response.body().byteStream();
                         OutputStream out = new FileOutputStream(outputFile)) {
                        
                        byte[] buffer = new byte[8192];
                        long downloadedBytes = 0;
                        int bytesRead;
                        
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            downloadedBytes += bytesRead;
                            
                            final int progress = (int) (downloadedBytes * 100 / totalBytes);
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progress);
                                progressBar.setString(String.format(bundle.getString("download_progress"), progress));
                            });
                        }
                    }
                    
                    // Update JAR path
                    SwingUtilities.invokeLater(() -> {
                        jarPathField.setText(outputFile.getAbsolutePath());
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this,
                            bundle.getString("download_success"),
                            bundle.getString("success"),
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this,
                            String.format(bundle.getString("download_failed"), e.getMessage()),
                            bundle.getString("error"),
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            
            progressDialog.setVisible(true);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                String.format(bundle.getString("failed_start_download"), e.getMessage()),
                bundle.getString("error"),
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 