package com.github.thkwag.thymelab.launcher.ui.components;

import com.github.thkwag.thymelab.launcher.ui.MainForm;

import javax.swing.*;
import java.util.ResourceBundle;

public class MainMenuBar extends JMenuBar {
    private final JMenu toolsMenu;
    private final JMenu helpMenu;
    private final JMenuItem programSettingsMenuItem;
    private final JMenuItem thymeleafSettingsMenuItem;
    private final JMenuItem aboutMenuItem;
    private final JMenuItem exitMenuItem;

    public MainMenuBar(MainForm mainForm, ResourceBundle bundle) {
        toolsMenu = new JMenu(bundle.getString("menu_tools"));
        helpMenu = new JMenu(bundle.getString("menu_help"));
        
        programSettingsMenuItem = new JMenuItem(bundle.getString("menu_program_settings"));
        thymeleafSettingsMenuItem = new JMenuItem(bundle.getString("menu_thymeleaf_settings"));
        aboutMenuItem = new JMenuItem(bundle.getString("menu_about"));
        
        toolsMenu.add(programSettingsMenuItem);
        toolsMenu.add(thymeleafSettingsMenuItem);
        toolsMenu.addSeparator();
        
        exitMenuItem = new JMenuItem(bundle.getString("menu_exit"));
        exitMenuItem.addActionListener(e -> System.exit(0));
        toolsMenu.add(exitMenuItem);
        
        helpMenu.add(aboutMenuItem);
        
        add(toolsMenu);
        add(helpMenu);
    }

    public JMenuItem getProgramSettingsMenuItem() {
        return programSettingsMenuItem;
    }

    public JMenuItem getThymeleafSettingsMenuItem() {
        return thymeleafSettingsMenuItem;
    }

    public JMenuItem getAboutMenuItem() {
        return aboutMenuItem;
    }

    public JMenuItem getExitMenuItem() {
        return exitMenuItem;
    }

    public JMenu getToolsMenu() { return toolsMenu; }
    public JMenu getHelpMenu() { return helpMenu; }

    public void updateTexts(ResourceBundle bundle) {
        toolsMenu.setText(bundle.getString("menu_tools"));
        helpMenu.setText(bundle.getString("menu_help"));
        programSettingsMenuItem.setText(bundle.getString("menu_program_settings"));
        thymeleafSettingsMenuItem.setText(bundle.getString("menu_thymeleaf_settings"));
        aboutMenuItem.setText(bundle.getString("menu_about"));
        exitMenuItem.setText(bundle.getString("menu_exit"));
    }
} 