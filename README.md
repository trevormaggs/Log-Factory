# LogFactory

A lightweight Java 8 logging wrapper built on top of Java Util Logging (JUL).

LogFactory provides a simple application-wide logging solution that routes all log messages into a single file while supporting:

* INFO, WARN, ERROR, DEBUG and TRACE logging
* Configurable verbosity levels
* Built-in `AppFormatter`
* Custom formatter support
* Detailed exception diagnostics
* Thread-safe logger creation
* Zero external dependencies

## Features

### One Log File Per Application

Configure logging once and every logger created through `LogFactory` automatically writes to the same file.

```java
LogFactory.configure("application.log");
```

Architecture:

```text
Application
    |
    +-- Root Logger
            |
            +-- One FileHandler
```

### Simple Logger Access

```java
private static final LogFactory LOGGER = LogFactory.getLogger(MyClass.class);
```

### Standard Logging

```java
LOGGER.info("Application started");

LOGGER.warn("Configuration file not found");

LOGGER.error("Unable to process image");
```

### Debug Logging

```java
LogFactory.setDebug(true);

LOGGER.debug("Reading metadata block");
```

### Trace Logging

```java
LogFactory.setTrace(true);

LOGGER.trace("Entering TIFF parser");
```

### Exception Diagnostics

```java
try
{
    processImage();
}

catch(Exception exc)
{
    LOGGER.error("Image processing failed", exc);
}
```

When trace mode is enabled, `AppFormatter` generates a detailed diagnostic report containing:

* Exception type
* Exception message
* Full stack trace
* Nested causes
* Circular-reference protection

### Verbosity Levels

Control the amount of informational output written to the log.

```java
LogFactory.setVerbosityLevel(Verbosity.MEDIUM);
```

Available levels:

| Level  | Description                     |
| ------ | ------------------------------- |
| SIMPLE | Basic messages only             |
| MEDIUM | Standard operational details    |
| FULL   | Complete diagnostic information |

Example:

```java
LOGGER.info("Application started", Verbosity.SIMPLE);

LOGGER.info("Scanning folder", Verbosity.MEDIUM);

LOGGER.info("Reading EXIF metadata", Verbosity.FULL);
```

## Getting Started

Configure logging once during application startup:

```java
LogFactory.configure("application.log");
```

Or explicitly enable debug and disable trace modes:

```java
LogFactory.configure("application.log", true, false); 
```

## Default Behaviour

After configuration:

* All loggers write to a single shared log file.
* Existing log files are appended to rather than overwritten.
* The built-in `AppFormatter` is used by default.
* `Verbosity.SIMPLE` is the default verbosity level.
* Debug logging is disabled by default.
* Trace logging is disabled by default.

## Custom Formatter

You may supply your own JUL formatter:

```java
LogFactory.configure("application.log", new MyFormatter());
```

## Example

```java
public class Main
{
    private static final LogFactory LOGGER = LogFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        try
        {
            LogFactory.configure("application.log");

            LOGGER.info("Application started");
        }

        catch(IOException exc)
        {
            exc.printStackTrace();
        }
    }
}
```

## Requirements

* Java 8 or later
* No external dependencies

## Credits

Developed and maintained by **Trevor Maggs**.

Bug reports, suggestions and feedback are welcome.

**Email:** [trevmaggs@tpg.com.au](mailto:trevmaggs@tpg.com.au)

## Licence

Currently proprietary. An open-source licence may be added in a future release.

---
