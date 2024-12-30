package com.github.thkwag.thymelab.launcher.ui.dialogs;

import com.github.thkwag.thymelab.launcher.config.ConfigManager;
import com.github.thkwag.thymelab.launcher.config.ConfigManager.LanguageChangeListener;
import com.github.thkwag.thymelab.launcher.util.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

public class AboutDialog extends JDialog implements LanguageChangeListener {
    
    // Layout constants
    private static final int BORDER_PADDING = 20;
    private static final int ICON_SIZE = 64;
    private static final int TITLE_FONT_SIZE = 16;
    private static final int LIBRARIES_FONT_SIZE = 12;
    private static final int LIBRARIES_WIDTH = 350;
    private static final int LIBRARIES_HEIGHT = 120;
    private static final int VERTICAL_SPACING_LARGE = 15;
    private static final int VERTICAL_SPACING_MEDIUM = 10;
    private static final int VERTICAL_SPACING_SMALL = 5;
    private static final int VERTICAL_SPACING_TINY = 2;
    private static final int TEXT_AREA_PADDING = 5;

    // Color settings
    private static final Color LINK_COLOR = new Color(100, 100, 100);
    private static final Color LIBRARIES_BACKGROUND = new Color(250, 250, 250);
    private static final Color LIBRARIES_BORDER = new Color(240, 240, 240);

    // Resource paths
    private static final String ICON_PATH = "/icons/icon.png";
    private static final String LIBRARIES_PATH = "/libraries.txt";
    
    private ResourceBundle bundle;
    private final JLabel titleLabel;
    private final JLabel subTitleLabel;
    private final JLabel copyrightLabel;
    private final JLabel linkLabel;
    private final JButton closeButton;

    public AboutDialog(Frame parent, ResourceBundle bundle, ConfigManager config) {
        super(parent, bundle.getString("about_title"), true);
        this.bundle = bundle;
        config.addLanguageChangeListener(this);
        setResizable(false);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(
            BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        
        try {
            InputStream iconStream = getClass().getResourceAsStream(ICON_PATH);
            if (iconStream != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(iconStream));
                Image scaledImage = icon.getImage().getScaledInstance(
                    ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
                iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(iconLabel);
                panel.add(Box.createVerticalStrut(VERTICAL_SPACING_LARGE));
            }
        } catch (Exception ex) {
            AppLogger.error(ex.getMessage(), ex);
        }
        
        titleLabel = new JLabel(bundle.getString("app_title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        subTitleLabel = new JLabel(bundle.getString("sub_title"));
        subTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel versionLabel = new JLabel("Version " + config.getVersion());
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        copyrightLabel = new JLabel(bundle.getString("about_copyright"));
        copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel licenseLabel = new JLabel("MIT License");
        licenseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        licenseLabel.setForeground(LINK_COLOR);
        
        linkLabel = new JLabel("<html><a href='" + bundle.getString("about_link") + "'>" + 
            bundle.getString("about_link") + "</a></html>");
        linkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(bundle.getString("about_link")));
                } catch (Exception ex) {
                    AppLogger.error(ex.getMessage(), ex);
                }
            }
        });
        
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_MEDIUM));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_MEDIUM));
        panel.add(subTitleLabel);
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_SMALL));
        panel.add(versionLabel);
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_SMALL));
        panel.add(copyrightLabel);
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_TINY));
        panel.add(licenseLabel);
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_MEDIUM));
        
        // Add library information
        try (InputStream inputStream = getClass().getResourceAsStream(LIBRARIES_PATH)) {
            if (inputStream != null) {
                String librariesContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                JTextArea librariesText = new JTextArea(librariesContent);
                librariesText.setEditable(false);
                librariesText.setBackground(LIBRARIES_BACKGROUND);
                librariesText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, LIBRARIES_FONT_SIZE));
                librariesText.setAlignmentX(Component.CENTER_ALIGNMENT);
                librariesText.setBorder(BorderFactory.createEmptyBorder(
                    TEXT_AREA_PADDING, TEXT_AREA_PADDING, 
                    TEXT_AREA_PADDING, TEXT_AREA_PADDING));
                
                // Add text area to scroll pane
                JScrollPane scrollPane = new JScrollPane(librariesText);
                scrollPane.setPreferredSize(new Dimension(LIBRARIES_WIDTH, LIBRARIES_HEIGHT));
                scrollPane.setBorder(BorderFactory.createLineBorder(LIBRARIES_BORDER));
                scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
                
                panel.add(scrollPane);
            }
        } catch (IOException ex) {
            AppLogger.error(ex.getMessage(), ex);
        }
        
        // Use panel to center GitHub link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkPanel.add(linkLabel);
        linkPanel.setOpaque(false);
        
        panel.add(Box.createVerticalStrut(VERTICAL_SPACING_MEDIUM));
        panel.add(linkPanel);
        panel.add(Box.createVerticalStrut(BORDER_PADDING));
        
        closeButton = new JButton(bundle.getString("about_close"));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> setVisible(false));
        panel.add(closeButton);
        
        add(panel);
        pack();
        setLocationRelativeTo(parent);
    }

    @Override
    public void onLanguageChange(String languageCode) {
        this.bundle = ResourceBundle.getBundle("i18n/messages", 
            Locale.forLanguageTag(languageCode), this.getClass().getClassLoader());
        updateTexts();
    }

    private void updateTexts() {
        setTitle(bundle.getString("about_title"));
        titleLabel.setText(bundle.getString("app_title"));
        subTitleLabel.setText(bundle.getString("sub_title"));
        copyrightLabel.setText(bundle.getString("about_copyright"));
        linkLabel.setText("<html><a href='" + bundle.getString("about_link") + "'>" + 
            bundle.getString("about_link") + "</a></html>");
        closeButton.setText(bundle.getString("about_close"));
    }
} 