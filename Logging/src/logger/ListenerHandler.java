package logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Lightweight handler that routes JUL log records to registered application listeners. When no
 * listeners are registered, log records are ignored without formatting or dispatch.
 */
public class ListenerHandler extends Handler
{
    private static final List<LogListener> LISTENERS = new CopyOnWriteArrayList<LogListener>();

    /**
     * Registers a listener to receive published log records.
     *
     * @param listener
     *        the listener to register, skipped if {@code null} is received
     */
    public static void addListener(LogListener listener)
    {
        if (listener != null)
        {
            LISTENERS.add(listener);
        }
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener
     *        the listener to remove
     */
    public static void removeListener(LogListener listener)
    {
        LISTENERS.remove(listener);
    }

    @Override
    public void publish(LogRecord record)
    {
        if (!LISTENERS.isEmpty() && isLoggable(record))
        {
            String msg = (getFormatter() != null) ? getFormatter().format(record) : record.getMessage();

            for (LogListener listener : LISTENERS)
            {
                try
                {
                    listener.onLog(record.getLevel(), msg);
                }

                catch (Throwable exc)
                {
                    System.err.println("LogListener failure: " + exc.getMessage());
                }
            }
        }
    }

    /**
     * Performs no action because this handler does not buffer log records.
     */
    @Override
    public void flush()
    {
    }

    /**
     * Performs no action because this handler does not own any resources that require closing.
     *
     * @throws SecurityException
     *         if a security manager denies the close operation
     */
    @Override
    public void close() throws SecurityException
    {
    }
}