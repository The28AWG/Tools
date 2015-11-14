package ru.tasp.tools.logger;

/**
 * Created by the28awg on 20.10.15.
 */
public interface Printer {

    Printer t(String tag, int methodCount);

    Settings init(String tag);

    Settings getSettings();

    void debug(String message, Object... args);

    void error(String message, Object... args);

    void error(Throwable throwable, String message, Object... args);

    void warn(String message, Object... args);

    void info(String message, Object... args);

    void trace(String message, Object... args);

    void fatal(String message, Object... args);

    void clear();

    boolean isTrace();

    boolean isDebug();

    boolean isInfo();

    boolean isWarn();

    boolean isError();

    boolean isFatal();
}