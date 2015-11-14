package ru.tasp.tools.logger;

/**
 * Created by the28awg on 20.10.15.
 */
public enum LogLevel {
    TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), FATAL(5);

    private int levelIndicator;

    LogLevel(int level) {
        levelIndicator = level;
    }

    public int getValue() {
        return levelIndicator;
    }
}