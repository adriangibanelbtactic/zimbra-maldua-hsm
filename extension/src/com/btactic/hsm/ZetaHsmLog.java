package com.btactic.hsm;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;

public class ZetaHsmLog {
    private static final String PREFIX = "[ZetaHsm] ";
    private static final Log log = ZimbraLog.extensions;

    private ZetaHsmLog() {
        // Prevent instantiation
    }

    // TRACE
    public static void trace(Object o) {
        log.trace(PREFIX + o);
    }

    public static void trace(Object o, Throwable t) {
        log.trace(PREFIX + o, t);
    }

    public static void trace(String format, Object... args) {
        log.trace(PREFIX + format, args);
    }

    // DEBUG
    public static void debug(Object o) {
        log.debug(PREFIX + o);
    }

    public static void debug(Object o, Throwable t) {
        log.debug(PREFIX + o, t);
    }

    public static void debug(String format, Object... args) {
        log.debug(PREFIX + format, args);
    }

    // INFO
    public static void info(Object o) {
        log.info(PREFIX + o);
    }

    public static void info(Object o, Throwable t) {
        log.info(PREFIX + o, t);
    }

    public static void info(String format, Object... args) {
        log.info(PREFIX + format, args);
    }

    // WARN
    public static void warn(Object o) {
        log.warn(PREFIX + o);
    }

    public static void warn(Object o, Throwable t) {
        log.warn(PREFIX + o, t);
    }

    public static void warn(String format, Object... args) {
        log.warn(PREFIX + format, args);
    }

    public static void warnQuietly(Object o, Throwable t) {
        log.warnQuietly(PREFIX + o, t);
    }

    public static void warnQuietlyFmt(String format, Object o) {
        log.warnQuietlyFmt(PREFIX + format, o);
    }

    public static void warnQuietly(String format, Object o, Throwable t) {
        log.warnQuietly(PREFIX + format, o, t);
    }

    // ERROR
    public static void error(Object o) {
        log.error(PREFIX + o);
    }

    public static void error(Object o, Throwable t) {
        log.error(PREFIX + o, t);
    }

    public static void error(String format, Object... args) {
        log.error(PREFIX + format, args);
    }

    public static void errorQuietly(Object o, Throwable t) {
        log.errorQuietly(PREFIX + o, t);
    }

    public static void errorQuietlyFmt(String format, Object o) {
        log.errorQuietlyFmt(PREFIX + format, o);
    }

    public static void errorQuietly(String format, Object o, Throwable t) {
        log.errorQuietly(PREFIX + format, o, t);
    }

    // FATAL
    public static void fatal(Object o) {
        log.fatal(PREFIX + o);
    }

    public static void fatal(Object o, Throwable t) {
        log.fatal(PREFIX + o, t);
    }

    public static void fatal(String format, Object... args) {
        log.fatal(PREFIX + format, args);
    }

    // IS ENABLED CHECKS
    public static boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    public static boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public static boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public static boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public static boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public static boolean isFatalEnabled() {
        return log.isFatalEnabled();
    }
}
