# LogFactory

A lightweight Java 8 logging wrapper built on top of Java Util Logging (JUL).

LogFactory provides a simple application-wide logging solution that routes all log messages into a single file while supporting:

* INFO, WARNING, ERROR, DEBUG and TRACE logging
* Configurable verbosity levels
* Custom log formatting
* Detailed exception diagnostics
* Thread-safe logger creation
* Single-file application logging

## Features

### One Log File Per Application

Configure logging once and every logger created through `LogFactory` automatically writes to the same file.

```java
LogFactory.configure("application.log", true, false);
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

When trace mode is enabled, the formatter generates a detailed diagnostic report containing:

* exception type
* exception message
* stack trace
* nested causes
* circular reference protection

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

## Example

```java
public class Main
{
    private static final LogFactory LOGGER = LogFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        try
        {
            LogFactory.configure("application.log", true, false);

            LOGGER.info("Application started");

            LOGGER.debug("Debug mode enabled");
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


## ✍️ Credits

This library is developed and maintained by **Trevor Maggs**.

Anyone wishing to use this resource is welcome to download or clone the repository via Git. If you have any comments, suggestions, or find any bugs, please direct your questions to me via email: **[trevmaggs@tpg.com.au](mailto:trevmaggs@tpg.com.au)**.


## Licence
Internal / Proprietary. A proper licence type will be added.

---

