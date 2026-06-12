package logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A centralised formatter for Java Util Logging (JUL) that produces structured log output and
 * enhanced diagnostic information for exceptions.
 *
 * <p>
 * Standard log messages are rendered as single-line entries containing a severity label, timestamp,
 * and message text. Log records associated with exceptions are expanded into a multi-line
 * diagnostic report containing exception details, stack traces, and nested causes.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 11 June 2026
 */
public class AppFormatter extends Formatter
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(":dd/MM/yyyy HH.mm.ss").withZone(ZoneId.systemDefault());

    /**
     * Defines the application's custom severity labels and their corresponding JUL level values.
     *
     * <p>
     * These labels are used when rendering log output so that JUL levels such as
     * {@link Level#CONFIG} and {@link Level#FINE} appear as application-specific severity names.
     * </p>
     */
    public enum CustomSeverity
    {
        TRACE(500), DEBUG(700), INFO(800), WARN(900), ERROR(1000);

        private final int value;
        private static final Map<Integer, CustomSeverity> SEVERITY_MAP = new ConcurrentHashMap<>();

        static
        {
            for (CustomSeverity severity : values())
            {
                SEVERITY_MAP.put(severity.getValue(), severity);
            }
        }

        private CustomSeverity(int value)
        {
            this.value = value;
        }

        /**
         * Returns the JUL integer value associated with this severity level.
         *
         * @return the underlying JUL level value
         */
        public int getValue()
        {
            return value;
        }

        /**
         * Returns the custom severity associated with the supplied JUL level value.
         *
         * @param k
         *        the JUL level integer value
         * @return the corresponding severity, or {@code null} if no mapping exists
         */
        public static CustomSeverity getSeverity(int k)
        {
            return SEVERITY_MAP.get(k);
        }
    }

    /**
     * Formats a log record into the application's standard output layout.
     *
     * <p>
     * Records without an associated exception are rendered as a single-line log entry. Records
     * containing a {@link Throwable} are expanded into a detailed diagnostic report including stack
     * trace and cause information.
     * </p>
     *
     * @param record
     *        the log record to format
     * @return the formatted log output
     */
    @Override
    public String format(LogRecord record)
    {
        StringBuilder logBuilder = new StringBuilder(1024);

        if (record.getThrown() == null)
        {
            String message = record.getMessage();

            if (message != null && !message.isEmpty())
            {
                appendHeader(logBuilder, record);
                logBuilder.append(formatMessage(record));
                logBuilder.append(System.lineSeparator());
            }
        }

        else
        {
            addDiagnosticTraceDump(logBuilder, record);
            logBuilder.append(System.lineSeparator());
        }

        return logBuilder.toString();
    }

    /**
     * Appends the common log entry prefix consisting of the severity label, timestamp, and
     * alignment spacing.
     *
     * @param sb
     *        destination builder
     * @param record
     *        log record supplying level and timestamp information
     */
    private void appendHeader(StringBuilder sb, LogRecord record)
    {
        CustomSeverity severity = CustomSeverity.getSeverity(record.getLevel().intValue());
        String sevName = (severity != null) ? severity.toString() : record.getLevel().getName();

        sb.append(sevName);
        sb.append(DATE_FORMATTER.format(Instant.ofEpochMilli(record.getMillis())));

        // Adjust spacing so that log messages align regardless of severity label length
        if (record.getLevel() == Level.INFO || record.getLevel() == Level.WARNING)
        {
            sb.append(String.format("%-4s", ":"));
        }

        else
        {
            sb.append(String.format("%-3s", ":"));
        }
    }

    /**
     * Generates a multi-line diagnostic report for a log record containing an exception.
     *
     * <p>
     * The report includes:
     * </p>
     *
     * <ul>
     * <li>the original log message</li>
     * <li>the exception type</li>
     * <li>the exception message</li>
     * <li>the complete stack trace</li>
     * <li>all nested causes</li>
     * </ul>
     *
     * <p>
     * Circular exception references are detected and safely terminated to prevent infinite
     * processing.
     * </p>
     *
     * @param sb
     *        destination builder
     * @param record
     *        log record containing exception information
     */
    private void addDiagnosticTraceDump(StringBuilder sb, LogRecord record)
    {
        Throwable exc = record.getThrown();
        Throwable cause = exc.getCause();
        StackTraceElement[] elements = exc.getStackTrace();
        Set<Throwable> seenExceptions = Collections.newSetFromMap(new IdentityHashMap<>());

        appendHeader(sb, record);
        sb.append("************** Diagnostic Stack Trace Information **************");
        sb.append(System.lineSeparator());

        appendHeader(sb, record);
        sb.append("  Message:\t\t");
        sb.append(formatMessage(record));
        sb.append(System.lineSeparator());

        String excMessage = (exc.getMessage() != null) ? exc.getMessage() : "No detail message";

        if (excMessage.contains("\n"))
        {
            for (String str : excMessage.split("\n"))
            {
                if (!str.isEmpty())
                {
                    appendHeader(sb, record);
                    sb.append("  Reason:\t\t").append(str.trim());
                    sb.append(System.lineSeparator());
                }
            }
        }

        else
        {
            appendHeader(sb, record);
            sb.append("  Reason:\t\t").append(excMessage);
            sb.append(System.lineSeparator());
        }

        appendHeader(sb, record);
        sb.append("\tException [").append(exc.getClass().getCanonicalName()).append("] was escalated in class [").append(record.getLoggerName()).append("]");
        sb.append(System.lineSeparator());

        for (StackTraceElement trace : elements)
        {
            appendHeader(sb, record);
            sb.append(String.format("%4sJava file [%s], Method [%s], Line Number [%s]", "", trace.getFileName(), trace.getMethodName(), trace.getLineNumber()));
            sb.append(System.lineSeparator());
        }

        // Block circular cause loops if any
        seenExceptions.add(exc);

        // Iteratively handle "Caused by" information safely
        while (cause != null)
        {
            boolean referenceFound = !seenExceptions.add(cause);

            if (referenceFound)
            {
                appendHeader(sb, record);
                sb.append("  [Circular exception reference detected. Loop terminated to prevent crash]");
                sb.append(System.lineSeparator());
                break;
            }

            appendHeader(sb, record);
            sb.append("  Caused by:\t").append(cause.getClass().getCanonicalName());
            sb.append(System.lineSeparator());

            String causeMsg = (cause.getMessage() != null) ? cause.getMessage() : "No detail message";

            if (causeMsg.contains("\n"))
            {
                for (String str : causeMsg.split("\n"))
                {
                    if (!str.isEmpty())
                    {
                        appendHeader(sb, record);
                        sb.append("  Reason:\t\t").append(str.trim());
                        sb.append(System.lineSeparator());
                    }
                }
            }

            else
            {
                appendHeader(sb, record);
                sb.append("  Reason:\t\t").append(causeMsg);
                sb.append(System.lineSeparator());
            }

            for (StackTraceElement trace : cause.getStackTrace())
            {
                appendHeader(sb, record);
                sb.append(String.format("%4sJava file [%s], Method [%s], Line Number [%s]", "", trace.getFileName(), trace.getMethodName(), trace.getLineNumber()));
                sb.append(System.lineSeparator());
            }

            cause = cause.getCause();
        }

        appendHeader(sb, record);

        sb.append("****************************************************************");
    }
}