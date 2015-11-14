package ru.tasp.tools.logger;

/**
 * Created by the28awg on 20.10.15.
 */
public final class L {
    protected static final String DEFAULT_TAG = "ru.tasp";

    private static Printer printer = new LoggerPrinter();

    //no instance
    private L() {
    }

    /**
     * It is used to get the settings object in order to change settings
     *
     * @return the settings object
     */
    public static Settings init() {
        return init(DEFAULT_TAG);
    }

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in Logger as TAG
     */
    public static Settings init(String tag) {
        printer = new LoggerPrinter();
        return printer.init(tag);
    }

    public static void clear() {
        printer.clear();
        printer = null;
    }

    public static Printer t(String tag) {
        return printer.t(tag, printer.getSettings().getMethodCount());
    }

    public static Printer t(int methodCount) {
        return printer.t(null, methodCount);
    }

    public static Printer t(String tag, int methodCount) {
        return printer.t(tag, methodCount);
    }

    public static void debug(String message, Object... args) {
        printer.debug(message, args);
    }

    public static void error(String message, Object... args) {
        printer.error(null, message, args);
    }

    public static void error(Throwable throwable, String message, Object... args) {
        printer.error(throwable, message, args);
    }

    public static void info(String message, Object... args) {
        printer.info(message, args);
    }

    public static void trace(String message, Object... args) {
        printer.trace(message, args);
    }

    public static void warn(String message, Object... args) {
        printer.warn(message, args);
    }

    public static void fatal(String message, Object... args) {
        printer.fatal(message, args);
    }

    private static boolean isLoggable(LogLevel logLevel) {
        return (printer.getSettings().getLogLevel().getValue() <= logLevel.getValue());
    }

    public static boolean isTrace() {
        return isLoggable(LogLevel.TRACE);
    }

    public static boolean isDebug() {
        return isLoggable(LogLevel.DEBUG);
    }

    public static boolean isInfo() {
        return isLoggable(LogLevel.INFO);
    }

    public static boolean isWarn() {
        return isLoggable(LogLevel.WARN);
    }

    public static boolean isError() {
        return isLoggable(LogLevel.ERROR);
    }

    public static boolean isFatal() {
        return isLoggable(LogLevel.FATAL);
    }
}