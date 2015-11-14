package ru.tasp.tools.logger;

/**
 * Logger is a wrapper for logging utils
 * But more pretty, simple and powerful
 */
final class LoggerPrinter implements Printer {
    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 3;

    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '╔';
    private static final char BOTTOM_LEFT_CORNER = '╚';
    private static final char MIDDLE_CORNER = '╟';
    private static final char HORIZONTAL_DOUBLE_LINE = '║';
    private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
    private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;
    /**
     * Localize single tag and method count for each thread
     */
    private final ThreadLocal<String> localTag = new ThreadLocal<>();
    private final ThreadLocal<Integer> localMethodCount = new ThreadLocal<>();
    /**
     * tag is used for the Log, the name is a little different
     * in order to differentiate the logs easily with the filter
     */
    private String tag;
    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private Settings settings;

    private static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    private static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in Logger
     */
    @Override
    public Settings init(String tag) {
        if (tag == null) {
            throw new NullPointerException("tag may not be null");
        }
        if (tag.trim().length() == 0) {
            throw new IllegalStateException("tag may not be empty");
        }
        this.tag = tag;
        this.settings = new Settings();
        return settings;
    }

    @Override
    public Settings getSettings() {
        if (settings == null) {
            init(L.DEFAULT_TAG);
        }
        return settings;
    }

    @Override
    public Printer t(String tag, int methodCount) {
        if (tag != null) {
            localTag.set(tag);
        }
        localMethodCount.set(methodCount);
        return this;
    }

    @Override
    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, args);
    }

    @Override
    public void error(String message, Object... args) {
        error(null, message, args);
    }

    @Override
    public void error(Throwable throwable, String message, Object... args) {
        if (throwable != null && message != null) {
            message += " : " + throwable.toString();
        }
        if (throwable != null && message == null) {
            message = throwable.toString();
        }
        if (message == null) {
            message = "No message/exception is set";
        }
        log(LogLevel.ERROR, message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        log(LogLevel.WARN, message, args);
    }

    @Override
    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    @Override
    public void trace(String message, Object... args) {
        log(LogLevel.TRACE, message, args);
    }

    @Override
    public void fatal(String message, Object... args) {
        log(LogLevel.FATAL, message, args);
    }

    @Override
    public void clear() {
        settings = null;
    }

    /**
     * This method is synchronized in order to avoid messy of logs' order.
     */

    private boolean isLoggable(LogLevel logLevel) {
        return (this.settings.getLogLevel().getValue() <= logLevel.getValue());
    }
    private synchronized void log(LogLevel logType, String msg, Object... args) {
        if (settings == null) {
            init(L.DEFAULT_TAG);
        }
        if (isLoggable(logType)) {
            String tag = getTag();
            String message = createMessage(msg, args);
            int methodCount = getMethodCount();

            logTopBorder(logType, tag);
            logHeaderContent(logType, tag, methodCount);

            //get bytes of message with system's default charset (which is UTF-8 for Android)
            byte[] bytes = message.getBytes();
            int length = bytes.length;
            if (length <= CHUNK_SIZE) {
                if (methodCount > 0) {
                    logDivider(logType, tag);
                }
                logContent(logType, tag, message);
                logBottomBorder(logType, tag);
                return;
            }
            if (methodCount > 0) {
                logDivider(logType, tag);
            }
            for (int i = 0; i < length; i += CHUNK_SIZE) {
                int count = Math.min(length - i, CHUNK_SIZE);
                //create a new String with system's default charset (which is UTF-8 for Android)
                logContent(logType, tag, new String(bytes, i, count));
            }
            logBottomBorder(logType, tag);
        }
    }

    private void logTopBorder(LogLevel logType, String tag) {
        logChunk(logType, tag, TOP_BORDER);
    }

    private void logHeaderContent(LogLevel logType, String tag, int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (settings.isShowThreadInfo()) {
            logChunk(logType, tag, HORIZONTAL_DOUBLE_LINE + " Thread: " + Thread.currentThread().getName());
            logDivider(logType, tag);
        }
        String level = "";

        int stackOffset = getStackOffset(trace) + settings.getMethodOffset();

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

        for (int i = methodCount; i > 0; i--) {
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("║ ")
                    .append(level)
                    .append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append(".")
                    .append(trace[stackIndex].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[stackIndex].getFileName())
                    .append(":")
                    .append(trace[stackIndex].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logType, tag, builder.toString());
        }
    }

    private void logBottomBorder(LogLevel logType, String tag) {
        logChunk(logType, tag, BOTTOM_BORDER);
    }

    private void logDivider(LogLevel logType, String tag) {
        logChunk(logType, tag, MIDDLE_BORDER);
    }

    private void logContent(LogLevel logType, String tag, String chunk) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, HORIZONTAL_DOUBLE_LINE + " " + line);
        }
    }

    public boolean isTrace() {
        return isLoggable(LogLevel.TRACE);
    }

    public boolean isDebug() {
        return isLoggable(LogLevel.DEBUG);
    }

    public boolean isInfo() {
        return isLoggable(LogLevel.INFO);
    }

    public boolean isWarn() {
        return isLoggable(LogLevel.WARN);
    }

    public boolean isError() {
        return isLoggable(LogLevel.ERROR);
    }

    public boolean isFatal() {
        return isLoggable(LogLevel.FATAL);
    }
    private void logChunk(LogLevel logType, String tag, String chunk) {
        String finalTag = formatTag(tag);
        switch (logType) {
            case ERROR:
                settings.getLogTool().error(finalTag, chunk);
                break;
            case INFO:
                settings.getLogTool().info(finalTag, chunk);
                break;
            case TRACE:
                settings.getLogTool().trace(finalTag, chunk);
                break;
            case WARN:
                settings.getLogTool().warn(finalTag, chunk);
                break;
            case FATAL:
                settings.getLogTool().fatal(finalTag, chunk);
                break;
            case DEBUG:
                // Fall through, log debug by default
            default:
                settings.getLogTool().debug(finalTag, chunk);
                break;
        }
    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    private String formatTag(String tag) {
        if (!isEmpty(tag) && !equals(this.tag, tag)) {
            return this.tag + "-" + tag;
        }
        return this.tag;
    }

    /**
     * @return the appropriate tag based on local or global
     */
    private String getTag() {
        String tag = localTag.get();
        if (tag != null) {
            localTag.remove();
            return tag;
        }
        return this.tag;
    }

    private String createMessage(String message, Object... args) {
        return args.length == 0 ? message : String.format(message, args);
    }

    private int getMethodCount() {
        Integer count = localMethodCount.get();
        int result = settings.getMethodCount();
        if (count != null) {
            localMethodCount.remove();
            result = count;
        }
        if (result < 0) {
            throw new IllegalStateException("methodCount cannot be negative");
        }
        return result;
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(LoggerPrinter.class.getName()) && !name.equals(L.class.getName())) {
                return --i;
            }
        }
        return -1;
    }
}