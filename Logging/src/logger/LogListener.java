package logger;

import java.util.logging.Level;

/**
 * Defines a listener that receives log messages.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 29 July 2026
 */
public interface LogListener
{
    /**
     * Notifies the listener that a message has been logged.
     *
     * @param level
     *        the logging level
     * @param message
     *        the formatted log message
     */
    void onLog(Level level, String message);

    /**
     * Resets any internal state maintained by the listener, preparing it for reuse.
     */
    default void reset()
    {
        // Default no-op for stateless listeners.
    }
}