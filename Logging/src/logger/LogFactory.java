package logger;

import java.io.IOException;
import java.util.Objects;
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
 * @version 1.35
 * @since 30 July 2026
 */
public final class LogFactory
{
    private static final String MEDIUM_INDENT = "    ";
    private static final String FULL_INDENT = "        ";
    private static FileHandler appFileHandler = null;
    private static ListenerHandler appListenerHandler = null;
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
    }

    /**
     * Sets up the logging system to target a single log file using the default {@link AppFormatter}
     * formatter.
     *
     * <p>
     * Developers need to call this method once at the main entry point in their applications.
     * </p>
     *
     * @param logfile
     *        path to the target log file
     * 
     * @throws NullPointerException
     *         if logfile is null
     * @throws IOException
     *         if the file cannot be accessed or created
     */
    public static synchronized void configure(String logfile) throws IOException
    {
        Objects.requireNonNull(logfile, "Log file is undefined");
        configure(logfile, false, false);
    }

    /**
     * Sets up the logging system to target a single log file with options to enable debugging
     * and/or tracing.
     *
     * <p>
     * Developers need to call this method once at the main entry point in their applications.
     * </p>
     *
     * @param logfile
     *        path to the log file
     * @param debugEnabled
     *        true to turn on debug logging
     * @param traceEnabled
     *        true to turn on trace logging
     * 
     * @throws NullPointerException
     *         if logfile is null
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
     * This method configures the root logger with a shared {@link FileHandler} for file output and
     * a {@link ListenerHandler} for application listeners. All loggers created by this factory
     * automatically route their output through these handlers.
     * </p>
     *
     * <p>
     * If the service was previously configured, the existing handlers are detached and safely
     * closed before the new configuration is applied. The target {@link FileHandler} opens in
     * append mode, preserving existing file contents across application restarts.
     * </p>
     *
     * @param logfile
     *        path to the destination log file
     * @param debugEnabled
     *        true to enable debug logging
     * @param traceEnabled
     *        true to enable trace logging
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

        close();

        Logger rootLogger = Logger.getLogger("");
        debug = debugEnabled;
        trace = traceEnabled;

        rootLogger.setUseParentHandlers(false);

        for (Handler handler : rootLogger.getHandlers())
        {
            rootLogger.removeHandler(handler);

            try
            {
                handler.close();
            }

            catch (Exception exc)
            {
                // Safely pass through without noises
            }
        }

        appFileHandler = new FileHandler(logfile, true);
        appFileHandler.setFormatter(formatter);
        rootLogger.addHandler(appFileHandler);

        appListenerHandler = new ListenerHandler();
        appListenerHandler.setFormatter(formatter);
        rootLogger.addHandler(appListenerHandler);

        updateAllLoggers();
    }

    /**
     * Flushes, removes, and closes the handlers used by the logging service. After this method
     * returns, file logging is disabled until the logging service is configured again.
     */
    public static synchronized void close()
    {
        Logger rootLogger = Logger.getLogger("");

        if (appFileHandler != null)
        {
            try
            {
                rootLogger.removeHandler(appFileHandler);
                appFileHandler.flush();
                appFileHandler.close();
            }

            catch (Exception exc)
            {
                // Ignore closing exceptions during shutdown/cleanup
            }

            finally
            {
                appFileHandler = null;
            }
        }

        if (appListenerHandler != null)
        {
            try
            {
                rootLogger.removeHandler(appListenerHandler);
                appListenerHandler.flush();
                appListenerHandler.close();
            }

            catch (Exception exc)
            {
                // Ignore closing exceptions during shutdown/cleanup
            }

            finally
            {
                appListenerHandler = null;
            }
        }
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
        return new LogFactory(className);
    }

    /**
     * Turns debug logging on or off for the whole application.
     *
     * @param d
     *        true to turn on debug mode; false to keep it off
     */
    public static synchronized void setDebug(boolean d)
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
     * When turned on, highly detailed step-by-step messages will be written to the log file.
     * </p>
     *
     * @param t
     *        true to turn on trace mode, or false to disable it
     */
    public static synchronized void setTrace(boolean t)
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
     * Returns the currently configured global message verbosity level for indentation purposes.
     *
     * @return the active verbosity level
     */
    public static Verbosity getVerbosityLevel()
    {
        return logLevel;
    }

    /**
     * Adds a log listener to receive logging notifications.
     *
     * @param listener
     *        the listener to add
     */
    public static void addLogListener(LogListener listener)
    {
        ListenerHandler.addListener(listener);
    }

    /**
     * Removes a previously registered log listener.
     *
     * @param listener
     *        the listener to remove
     */
    public static void removeLogListener(LogListener listener)
    {
        ListenerHandler.removeListener(listener);
    }

    /**
     * Globally disables log record production across all application loggers.
     *
     */
    public static synchronized void disable()
    {
        Logger.getLogger("").setLevel(Level.OFF);
    }

    /**
     * Re-activates global logging, restoring active debug and trace level configurations.
     *
     */
    public static synchronized void enable()
    {
        updateAllLoggers();
    }

    /*
     * =====================================================
     * INSTANCE METHODS TO SUPPORT ONE SPECIFIC APPLICATION
     * =====================================================
     */

    /**
     * Logs an informational message formatted according to the specified verbosity level.
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
     *        the verbosity setting associated with the message
     *
     * @throws NullPointerException
     *         if sev is null
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
     * Logs an informational message.
     *
     * @param msg
     *        the message string to log
     */
    public void info(String msg)
    {
        log(Level.INFO, msg, null);
    }

    /**
     * Logs a debug-level message if debug mode is active.
     *
     * @param msg
     *        the message string to log
     */
    public void debug(String msg)
    {
        if (isDebugEnabled())
        {
            log(Level.CONFIG, msg, null);
        }
    }

    /**
     * Logs a warning-level message.
     *
     * @param msg
     *        the message string to log
     */
    public void warn(String msg)
    {
        log(Level.WARNING, msg, null);
    }

    /**
     * Logs an error-level message.
     *
     * @param msg
     *        the message string to log
     */
    public void error(String msg)
    {
        log(Level.SEVERE, msg, null);
    }

    /**
     * Logs an error message along with an associated exception stack trace.
     *
     * <p>
     * When trace mode is enabled, the exception is logged with full stack trace details. Otherwise
     * the exception is logged normally.
     * </p>
     *
     * @param msg
     *        the error description
     * @param exc
     *        exception or error thrown
     */
    public void error(String msg, Throwable exc)
    {
        if (isTraceEnabled())
        {
            trace(msg, exc);
        }

        else
        {
            log(Level.SEVERE, msg, exc);
        }
    }

    /**
     * Logs a fine-grained trace message if trace mode is active.
     *
     * @param msg
     *        the message string to log
     */
    public void trace(String msg)
    {
        if (isTraceEnabled())
        {
            log(Level.FINE, msg, null);
        }
    }

    /**
     * Logs a trace message along with an associated exception stack trace if trace mode is active.
     *
     * @param msg
     *        the message string to log
     * @param exc
     *        exception or error thrown
     */
    public void trace(String msg, Throwable exc)
    {
        if (isTraceEnabled())
        {
            log(Level.FINE, msg, exc);
        }
    }

    /**
     * Evaluates the current effective JUL logging level based on trace and debug toggles.
     *
     * @return the corresponding JUL logging Level
     */
    private static Level getCurrentLevel()
    {
        return (appFileHandler == null ? Level.OFF : (trace ? Level.FINE : (debug ? Level.CONFIG : Level.INFO)));
    }

    /**
     * Updates the root logger level and handler level configuration to reflect state changes.
     *
     */
    private static void updateAllLoggers()
    {
        Level targetLevel = getCurrentLevel();

        if (appFileHandler != null)
        {
            appFileHandler.setLevel(Level.ALL);
        }

        // Changing the root logger level instantly updates all child loggers
        Logger.getLogger("").setLevel(targetLevel);
    }

    /**
     * Internal helper method to despatch a log record to the underlying JUL logger.
     *
     * @param level
     *        severity level of the record
     * @param message
     *        log string
     * @param thrown
     *        exception or error thrown, or null if none
     */
    private void log(Level level, String message, Throwable thrown)
    {
        if (realLogger.isLoggable(level))
        {
            if (thrown != null)
            {
                realLogger.log(level, message, thrown);
            }

            else
            {
                realLogger.log(level, message);
            }
        }
    }
}