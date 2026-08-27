package logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Lightweight handler that routes JUL log records to active application listeners.
 * Operates as a zero-allocation no-op when no listeners are registered.
 */
public class ListenerHandler extends Handler
{
    private static final List<LogListener> LISTENERS = new CopyOnWriteArrayList<LogListener>();

    public static void addListener(LogListener listener)
    {
        if (listener != null)
        {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(LogListener listener)
    {
        LISTENERS.remove(listener);
    }

    @Override
    public void publish(LogRecord record)
    {
        if (!LISTENERS.isEmpty() && isLoggable(record))
        {
            String formattedMessage = (getFormatter() != null) ? getFormatter().format(record) : record.getMessage();

            for (LogListener listener : LISTENERS)
            {
                try
                {
                    listener.onLog(record.getLevel(), formattedMessage);
                }

                catch (Throwable exc)
                {
                    System.err.println("LogListener failure: " + exc.getMessage());
                }
            }
        }
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close() throws SecurityException
    {
    }
}