package com.github.thkwag.thymelab.launcher.ui.components;

import com.github.thkwag.thymelab.launcher.util.AppLogger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogPanel extends JPanel {
    private final JTextPane logTextPane;
    private final StyledDocument styledDoc;
    private final SimpleAttributeSet defaultStyle;
    private int maxBufferSize = DEFAULT_BUFFER_SIZE;

    // Default settings
    private static final int DEFAULT_BUFFER_SIZE = 1000;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final Color DEFAULT_BACKGROUND = Color.BLACK;
    private static final Color DEFAULT_FOREGROUND = new Color(229, 229, 229);  // WHITE

    // Font settings
    private static final String WINDOWS_FONT = "Consolas";
    private static final String MAC_FONT = "Menlo";
    private static final String LINUX_FONT = "DejaVu Sans Mono";

    // ANSI color settings
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");
    private static final Color[] ANSI_COLORS = {
        new Color(0, 0, 0),          // BLACK
        new Color(205, 0, 0),        // RED
        new Color(0, 205, 0),        // GREEN
        new Color(205, 205, 0),      // YELLOW
        new Color(0, 0, 238),        // BLUE
        new Color(205, 0, 205),      // MAGENTA
        new Color(0, 205, 205),      // CYAN
        new Color(229, 229, 229),    // WHITE
        // Bright colors
        new Color(127, 127, 127),    // BRIGHT BLACK (GRAY)
        new Color(255, 0, 0),        // BRIGHT RED
        new Color(0, 255, 0),        // BRIGHT GREEN
        new Color(255, 255, 0),      // BRIGHT YELLOW
        new Color(92, 92, 255),      // BRIGHT BLUE
        new Color(255, 0, 255),      // BRIGHT MAGENTA
        new Color(0, 255, 255),      // BRIGHT CYAN
        new Color(255, 255, 255)     // BRIGHT WHITE
    };

    // ANSI code offsets
    private static final int FOREGROUND_COLOR_OFFSET = 30;
    private static final int BACKGROUND_COLOR_OFFSET = 40;
    private static final int BRIGHT_COLOR_OFFSET = 82;  // 90-8
    private static final int BRIGHT_BG_COLOR_OFFSET = 92;  // 100-8

    // ANSI control codes
    private static final int ANSI_RESET = 0;
    private static final int ANSI_BOLD = 1;

    public LogPanel() {
        setLayout(new BorderLayout());
        
        logTextPane = new JTextPane();
        styledDoc = logTextPane.getStyledDocument();
        JScrollPane logScrollPane = new JScrollPane(logTextPane);
        
        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultStyle, DEFAULT_FOREGROUND);
        StyleConstants.setBackground(defaultStyle, DEFAULT_BACKGROUND);
        StyleConstants.setFontFamily(defaultStyle, getDefaultMonospacedFont());
        
        configureLogTextPane();
        add(logScrollPane, BorderLayout.CENTER);
    }

    public static String getDefaultMonospacedFont() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return WINDOWS_FONT;
        } else if (os.contains("mac")) {
            return MAC_FONT;
        } else {
            return LINUX_FONT;
        }
    }

    private void configureLogTextPane() {
        logTextPane.setEditable(false);
        logTextPane.setBackground(DEFAULT_BACKGROUND);
        logTextPane.setForeground(DEFAULT_FOREGROUND);
        logTextPane.setFont(new Font(getDefaultMonospacedFont(), Font.PLAIN, DEFAULT_FONT_SIZE));
        
        // Set default style
        StyleConstants.setForeground(defaultStyle, DEFAULT_FOREGROUND);
        StyleConstants.setBackground(defaultStyle, DEFAULT_BACKGROUND);
        StyleConstants.setFontFamily(defaultStyle, getDefaultMonospacedFont());
    }

    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                processAnsiText(text);
                trimBuffer();
                logTextPane.setCaretPosition(styledDoc.getLength());
            } catch (BadLocationException e) {
                AppLogger.error("Failed to append log", e);
            }
        });
    }

    private void processAnsiText(String text) throws BadLocationException {
        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastEnd = 0;
        SimpleAttributeSet currentStyle = new SimpleAttributeSet(defaultStyle);

        while (matcher.find()) {
            String textBeforeCode = text.substring(lastEnd, matcher.start());
            if (!textBeforeCode.isEmpty()) {
                styledDoc.insertString(styledDoc.getLength(), textBeforeCode, currentStyle);
            }

            String[] codes = matcher.group(1).split(";");
            for (String code : codes) {
                if (code.isEmpty()) continue;
                int value = Integer.parseInt(code);
                updateStyle(currentStyle, value);
            }
            
            lastEnd = matcher.end();
        }

        String remainingText = text.substring(lastEnd);
        if (!remainingText.isEmpty()) {
            styledDoc.insertString(styledDoc.getLength(), remainingText, currentStyle);
        }
    }

    private void updateStyle(SimpleAttributeSet style, int code) {
        switch (code) {
            case ANSI_RESET:  // Reset
                StyleConstants.setForeground(style, DEFAULT_FOREGROUND);
                StyleConstants.setBackground(style, DEFAULT_BACKGROUND);
                StyleConstants.setBold(style, false);
                break;
            case ANSI_BOLD:  // Bold
                StyleConstants.setBold(style, true);
                break;
            case 30: case 31: case 32: case 33:
            case 34: case 35: case 36: case 37:
                StyleConstants.setForeground(style, ANSI_COLORS[code - FOREGROUND_COLOR_OFFSET]);
                break;
            case 90: case 91: case 92: case 93:
            case 94: case 95: case 96: case 97:
                StyleConstants.setForeground(style, ANSI_COLORS[code - BRIGHT_COLOR_OFFSET]);
                break;
            case 40: case 41: case 42: case 43:
            case 44: case 45: case 46: case 47:
                StyleConstants.setBackground(style, ANSI_COLORS[code - BACKGROUND_COLOR_OFFSET]);
                break;
            case 100: case 101: case 102: case 103:
            case 104: case 105: case 106: case 107:
                StyleConstants.setBackground(style, ANSI_COLORS[code - BRIGHT_BG_COLOR_OFFSET]);
                break;
        }
    }

    private void trimBuffer() {
        try {
            String text = styledDoc.getText(0, styledDoc.getLength());
            int lineCount = text.split("\n").length;
            if (lineCount > maxBufferSize) {
                int toRemove = lineCount - maxBufferSize;
                int removePos = indexOfNthLineEnd(text, toRemove);
                if (removePos > 0) {
                    styledDoc.remove(0, removePos);
                }
            }
        } catch (BadLocationException e) {
            AppLogger.error(e.getMessage(), e);
        }
    }

    private int indexOfNthLineEnd(String text, int n) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
                if (count == n) return i + 1;
            }
        }
        return -1;
    }

    public void setMaxBufferSize(int size) {
        this.maxBufferSize = size;
        trimBuffer();
    }

    public void clearLog() {
        try {
            styledDoc.remove(0, styledDoc.getLength());
        } catch (BadLocationException e) {
            AppLogger.error("Failed to clear log", e);
        }
    }

    public void updateLogFont(String fontFamily, int fontSize) {
        Font newFont = new Font(fontFamily, Font.PLAIN, fontSize);
        logTextPane.setFont(newFont);
        
        // Update only font-related attributes
        SimpleAttributeSet fontStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(fontStyle, fontFamily);
        StyleConstants.setFontSize(fontStyle, fontSize);
        
        // Update font while preserving existing style attributes
        StyledDocument doc = logTextPane.getStyledDocument();
        Element root = doc.getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            AttributeSet attrs = elem.getAttributes();
            SimpleAttributeSet newAttrs = new SimpleAttributeSet(attrs);
            StyleConstants.setFontFamily(newAttrs, fontFamily);
            StyleConstants.setFontSize(newAttrs, fontSize);
            doc.setCharacterAttributes(elem.getStartOffset(), 
                elem.getEndOffset() - elem.getStartOffset(), newAttrs, false);
        }
        
        // Update default style
        StyleConstants.setFontFamily(defaultStyle, fontFamily);
        StyleConstants.setFontSize(defaultStyle, fontSize);
    }
} 