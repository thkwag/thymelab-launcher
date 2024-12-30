package com.github.thkwag.thymelab.launcher.process;

@FunctionalInterface
public interface LogConsumer {
    void accept(String line);
} 