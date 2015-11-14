package ru.tasp.tools.logger;

import android.util.Log;

/**
 * Created by the28awg on 25.10.15.
 */
public class AndroidLogTool implements LogTool {

    private long startMsecs;
    public AndroidLogTool() {
        this.startMsecs = System.currentTimeMillis();
    }
    @Override
    public void debug(String tag, String message) {
        log(tag, "DEBUG", message);
    }

    @Override
    public void error(String tag, String message) {
        logErr(tag, "ERROR", message);
    }

    @Override
    public void warn(String tag, String message) {
        log(tag, "WARN", message);
    }

    @Override
    public void info(String tag, String message) {
        log(tag, "INFO", message);
    }

    @Override
    public void trace(String tag, String message) {
        log(tag, "TRACE", message);
    }

    @Override
    public void fatal(String tag, String message) {
        log(tag, "FATAL", message);
    }

    private void log(String tag, String level, Object message) {
        Log.i(tag, String.format("[%5s]: %s", level, message));
    }

    private void logErr(String tag, String level, Object message) {
        Log.e(tag, String.format("[%5s]: %s", level, message));
    }

    private long getMsecs() {
        return System.currentTimeMillis() - startMsecs;
    }
}
