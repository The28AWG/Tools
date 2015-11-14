package ru.tasp.tools.logger;

/**
 * Created by the28awg on 20.10.15.
 */
public interface LogTool {
    void debug(String tag, String message);

    void error(String tag, String message);

    void warn(String tag, String message);

    void info(String tag, String message);

    void trace(String tag, String message);

    void fatal(String tag, String message);
}