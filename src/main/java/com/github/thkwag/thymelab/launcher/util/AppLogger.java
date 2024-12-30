package com.github.thkwag.thymelab.launcher.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AppLogger {
    private static final Logger logger;
    private static LogLevel currentLevel = LogLevel.INFO;

    static {
        logger = Logger.getLogger("ThymeLab");
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("[%s] %s%n", 
                    record.getLevel().toString(), 
                    record.getMessage());
            }
        });
        logger.addHandler(handler);
    }

    public enum LogLevel {
        ERROR("ERROR"),
        WARN("WARN"),
        INFO("INFO"),
        DEBUG("DEBUG");

        LogLevel(String level) {
        }

        public boolean isLoggable(LogLevel level) {
            return this.ordinal() >= level.ordinal();
        }

        public static LogLevel fromString(String level) {
            try {
                return valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return INFO;  // Default to INFO if invalid
            }
        }
    }

    public static void setLogLevel(String level) {
        currentLevel = LogLevel.fromString(level);
    }

    public static void info(String message) {
        if (currentLevel.isLoggable(LogLevel.INFO)) {
            logger.info(message);
        }
    }

    public static void error(String message) {
        if (currentLevel.isLoggable(LogLevel.ERROR)) {
            logger.severe(message);
        }
    }

    public static void error(String message, Throwable throwable) {
        if (currentLevel.isLoggable(LogLevel.ERROR)) {
            logger.severe(message + "\n" + throwable.toString());
        }
    }

    public static void warn(String message) {
        if (currentLevel.isLoggable(LogLevel.WARN)) {
            logger.warning(message);
        }
    }

    public static void debug(String message) {
        if (currentLevel.isLoggable(LogLevel.DEBUG)) {
            logger.fine(message);
        }
    }
} 