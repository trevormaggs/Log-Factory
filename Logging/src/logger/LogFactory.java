package logger;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A centralised logging factory wrapping {@link java.util.logging.Logger} (JUL). Configured
 * globally to route all log messages into a single target file.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 11 June 2026
 */
public final class LogFactory
{
    private static final Map<String, LogFactory> LOGGERS = new ConcurrentHashMap<>();
    private static final String MEDIUM_INDENT = "    ";
    private static final String FULL_INDENT = "        ";
    private static FileHandler appFileHandler = null;
    private static volatile boolean trace = false;
    private static volatile boolean debug = false;
    private static volatile Verbosity logLevel = Verbosity.SIMPLE;
    private final Logger realLogger;

    /**
     * Private constructor used to create a new logger for a specific class. Sets the initial
     * logging visibility based on the current global settings.
     *
     * @param className
     *        the name of the class using this logger
     */
    private LogFactory(String className)
    {
        this.realLogger = Logger.getLogger(className);
        this.realLogger.setLevel(getCurrentLevel());
    }

    /**
     * Sets up the logging system to use a single target log file using the default
     * {@link CustomFormatter} formatter. Developers need to call this method once at the main entry
     * point in their applications.
     * 
     * @param logfile
     *        path to the target log file
     * 
     * @throws IOException
     *         if the file cannot be accessed or created
     */
    public static synchronized void configure(String logfile) throws IOException
    {
        Objects.requireNonNull(logfile, "Log file is undefined");

        configure(logfile, false, false);
    }

    /**
     * Sets up the logging system to use a single log file.
     *
     * This method refreshes the logging configuration based on the global settings, preventing
     * double-logging or file locks. Every logger registered by this factory will automatically
     * route to the same specified file.
     * 
     * Developers need to call this method once at the main entry point in their applications.
     *
     * @param logfile
     *        path to the log file
     * @param debugEnabled
     *        true to turn on debug logging
     * @param traceEnabled
     *        true to turn on trace logging
     *
     * @throws NullPointerException
     *         if the file is null
     * @throws IOException
     *         if the file cannot be created or opened
     */
    public static synchronized void configure(String logfile, boolean debugEnabled, boolean traceEnabled) throws IOException
    {
        Objects.requireNonNull(logfile, "Log file is undefined");

        configure(logfile, debugEnabled, traceEnabled, new AppFormatter());
    }

    /**
     * Configures the logging service to write all application log messages to a single file using
     * the specified formatter.
     *
     * <p>
     * This factory sets up a <b>single-handler architecture</b> where the application Root Logger
     * manages exactly one {@link FileHandler}. Every logger you fetch from this factory will
     * automatically route its output to this single shared file.
     * </p>
     *
     * <p>
     * The Root Logger is configured using {@link Logger#setUseParentHandlers(boolean)} with a value
     * of {@code false} to ensure that log messages are processed only by the application's
     * configured handlers and are not forwarded beyond the application's logging hierarchy.
     * </p>
     *
     * <p>
     * Before the new file handler is attached, all existing handlers are removed and closed. This
     * prevents:
     * </p>
     *
     * <ul>
     * <li>duplicate log entries caused by multiple active handlers, eliminating potential
     * conflicts</li>
     * <li>resource leaks from stale file handles</li>
     * <li>unwanted output from the default JUL ConsoleHandler</li>
     * </ul>
     *
     * <p>
     * If the logging service has already been configured, the previously registered application
     * file handler is detached and closed before the handler is refreshed.
     * </p>
     *
     * <p>
     * The created {@link FileHandler} operates in append mode so that existing log contents are
     * preserved across application restarts.
     * </p>
     *
     * @param logfile
     *        path to the destination log file
     * @param debugEnabled
     *        true to turn on debug logging
     * @param traceEnabled
     *        true to turn on trace logging
     * @param formatter
     *        formatter used to render log records
     *
     * @throws NullPointerException
     *         if logfile or formatter is null
     * @throws IOException
     *         if the log file cannot be created or opened
     */
    public static synchronized void configure(String logfile, boolean debugEnabled, boolean traceEnabled, Formatter formatter) throws IOException
    {
        Objects.requireNonNull(logfile, "Log file is undefined");
        Objects.requireNonNull(formatter, "Formatter is undefined");

        Logger rootLogger = Logger.getLogger("");
        debug = debugEnabled;
        trace = traceEnabled;

        /*
         * Disable parent handler propagation so that log records are processed only by the handlers
         * explicitly registered on the Root Logger.
         */
        rootLogger.setUseParentHandlers(false);

        Handler[] handlers = rootLogger.getHandlers();

        for (Handler handler : handlers)
        {
            rootLogger.removeHandler(handler);
            handler.close();
        }

        if (appFileHandler != null)
        {
            rootLogger.removeHandler(appFileHandler);
            appFileHandler.close();
        }

        appFileHandler = new FileHandler(logfile, true);
        appFileHandler.setFormatter(formatter);
        appFileHandler.setLevel(Level.ALL);

        rootLogger.addHandler(appFileHandler);

        updateAllLoggers();
    }

    /**
     * Returns a logger associated with the specified class.
     *
     * <p>
     * The logger name is derived from the fully qualified class name. Repeated calls for the same
     * class return the same logger instance.
     * </p>
     *
     * @param clazz
     *        class used to derive the logger name
     * @return the corresponding logger instance
     *
     * @throws NullPointerException
     *         if clazz is null
     */
    public static LogFactory getLogger(Class<?> clazz)
    {
        Objects.requireNonNull(clazz, "Class is undefined");

        return getLogger(clazz.getName());
    }

    /**
     * Returns a logger associated with the specified name.
     *
     * <p>
     * If a logger with the specified name already exists, the existing instance is returned.
     * Otherwise a new logger is created and registered.
     * </p>
     *
     * @param className
     *        unique logger name
     * @return the corresponding logger instance
     *
     * @throws NullPointerException
     *         if className is null
     */
    public static LogFactory getLogger(String className)
    {
        Objects.requireNonNull(className, "Logger name is undefined");

        LogFactory logger = LOGGERS.get(className);

        if (logger == null)
        {
            logger = new LogFactory(className);

            LogFactory existing = LOGGERS.putIfAbsent(className, logger);

            if (existing != null)
            {
                logger = existing;
            }
        }

        return logger;
    }

    /**
     * Turns debug logging on or off for the whole application.
     *
     * <p>
     * When turned on, detailed troubleshooting messages will be saved to the log file. When turned
     * off, these extra messages are skipped to save space.
     * </p>
     *
     * @param d
     *        true to turn on debug mode; false to keep it off
     */
    public static void setDebug(boolean d)
    {
        debug = d;
        updateAllLoggers();
    }

    /**
     * Indicates whether debug logging is currently turned on.
     *
     * @return true if debug logging is enabled, otherwise false
     */
    public static boolean isDebugEnabled()
    {
        return debug;
    }

    /**
     * Turns deep trace logging on or off for the whole application.
     *
     * <p>
     * When turned on, highly detailed step-by-step messages will be saved to the log file.
     * </p>
     *
     * @param t
     *        true to turn on trace mode, or false to disable it
     */
    public static void setTrace(boolean t)
    {
        trace = t;
        updateAllLoggers();
    }

    /**
     * Indicates whether trace logging is currently turned on.
     *
     * @return true if trace logging is enabled, otherwise false
     */
    public static boolean isTraceEnabled()
    {
        return trace;
    }

    /**
     * Sets the global message layout verbosity level.
     *
     * <p>
     * Messages whose verbosity threshold is below the configured level will not be logged.
     * </p>
     *
     * @param level
     *        desired verbosity level setting
     *
     * @throws NullPointerException
     *         if level is null
     */
    public static void setVerbosityLevel(Verbosity level)
    {
        logLevel = Objects.requireNonNull(level, "Logger verbosity level is undefined");
    }

    /**
     * Returns the currently configured global message verbosity level.
     *
     * @return the active verbosity level
     */
    public static Verbosity getVerbosityLevel()
    {
        return logLevel;
    }

    /*
     * =====================================================
     * INSTANCE METHODS TO SUPPORT ONE SPECIFIC APPLICATION
     * =====================================================
     */

    /**
     * Enables logging for this specific logger using the current global logging settings.
     *
     * <p>
     * If this logger was previously disabled using {@link #disable()}, its logging functionality is
     * restored.
     * </p>
     */
    public void enable()
    {
        realLogger.setLevel(getCurrentLevel());
    }

    /**
     * Disables this logger until the next global level refresh or explicit call to enable().
     *
     * <p>
     * Only this logger instance is muted. Other loggers remain unchanged.
     * </p>
     */
    public void disable()
    {
        realLogger.setLevel(Level.OFF);
    }

    /**
     * Logs an informational message.
     *
     * @param msg
     *        message to log
     */
    public void info(String msg)
    {
        if (realLogger.isLoggable(Level.INFO))
        {
            realLogger.log(Level.INFO, msg);
        }
    }

    /**
     * Logs an informational message subject to the specified verbosity level.
     * 
     * <p>
     * The message is evaluated against the globally configured threshold. If the supplied verbosity
     * level falls within the active global verbosity threshold, the message is written using the
     * corresponding indentation level.
     * </p>
     *
     * @param msg
     *        the message string to log
     * @param sev
     *        the verbosity level classifying the structural importance of this message
     *
     * @throws NullPointerException
     *         if the provided verbosity level is null
     */
    public void info(String msg, Verbosity sev)
    {
        Objects.requireNonNull(sev, "Verbosity level is undefined");

        if (sev.getValue() <= logLevel.getValue())
        {
            switch (sev)
            {
                case FULL:
                    info(FULL_INDENT + msg);
                break;

                case MEDIUM:
                    info(MEDIUM_INDENT + msg);
                break;

                default:
                    info(msg);
                break;
            }
        }
    }

    /**
     * Logs a debug message.
     *
     * <p>
     * Messages are only written when debug mode is enabled.
     * </p>
     *
     * @param msg
     *        message to log
     */
    public void debug(String msg)
    {
        if (isDebugEnabled())
        {
            realLogger.log(Level.CONFIG, msg);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param msg
     *        message to log
     */
    public void warn(String msg)
    {
        if (realLogger.isLoggable(Level.WARNING))
        {
            realLogger.log(Level.WARNING, msg);
        }
    }

    /**
     * Logs an error message.
     *
     * @param msg
     *        message to log
     */
    public void error(String msg)
    {
        if (realLogger.isLoggable(Level.SEVERE))
        {
            realLogger.log(Level.SEVERE, msg);
        }
    }

    /**
     * Logs an error message associated with an exception.
     *
     * <p>
     * When trace mode is enabled, the exception is logged with full stack trace details. Otherwise
     * the exception is logged normally.
     * </p>
     *
     * @param msg
     *        message describing the error
     * @param exc
     *        associated exception
     */
    public void error(String msg, Throwable exc)
    {
        if (isTraceEnabled())
        {
            trace(msg, exc);
        }

        else
        {
            realLogger.log(Level.SEVERE, msg);
        }
    }

    /**
     * Logs a trace message.
     *
     * <p>
     * Messages are only written when trace mode is enabled.
     * </p>
     *
     * @param msg
     *        message to log
     */
    public void trace(String msg)
    {
        if (isTraceEnabled())
        {
            realLogger.log(Level.FINE, msg);
        }
    }

    /**
     * Logs a trace message together with an exception.
     *
     * <p>
     * The full stack trace is recorded when trace mode is enabled.
     * </p>
     *
     * @param msg
     *        message describing the event
     * @param exc
     *        associated exception
     */
    public void trace(String msg, Throwable exc)
    {
        if (isTraceEnabled())
        {
            realLogger.log(Level.FINE, msg, exc);
        }
    }

    /**
     * Internal helper to determine the current system logging threshold.
     */
    private static Level getCurrentLevel()
    {
        return (appFileHandler == null ? Level.OFF : (trace ? Level.FINE : (debug ? Level.CONFIG : Level.INFO)));        
    }

    /**
     * Synchronises the Root Logger and all registered LogFactory instances with the current global
     * debug and trace settings.
     */
    private static void updateAllLoggers()
    {
        Level targetLevel = getCurrentLevel();

        if (appFileHandler != null)
        {
            appFileHandler.setLevel(Level.ALL);
        }

        Logger.getLogger("").setLevel(targetLevel);

        for (LogFactory factory : LOGGERS.values())
        {
            factory.realLogger.setLevel(targetLevel);
        }
    }
}