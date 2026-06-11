package logger;

/**
 * Defines the application's layout verbosity thresholds for log output.
 * 
 * <p>
 * This enum uses sequential natural ordering where higher integer values represent increasing
 * levels of detail and inclusivity (e.g., {@code FULL} includes both {@code MEDIUM} and
 * {@code SIMPLE} logs).
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 11 June 2026
 */
public enum Verbosity
{
    /** Minimal layout verbosity containing only core log messages without indentation. */
    SIMPLE(0),

    /**
     * Intermediate layout verbosity containing moderately detailed messages and standard
     * indentation.
     */
    MEDIUM(1),

    /** Maximum layout verbosity containing comprehensive, deeply-indented structural details. */
    FULL(2);

    private final int value;
    private static final Verbosity[] CACHE = values();

    // Internal threshold value mapped to this verbosity level
    private Verbosity(int value)
    {
        this.value = value;
    }

    /**
     * Returns the internal integer value associated with this verbosity level.
     *
     * @return the integer threshold value
     */
    public int getValue()
    {
        return this.value;
    }

    /**
     * Safely maps an integer value to its corresponding {@code Verbosity} constant.
     * 
     * <p>
     * This lookup operates in constant (O(1)) time complexity by referencing a static array cache,
     * safely avoiding repeated heap allocations.
     * </p>
     *
     * @param k
     *        the integer value to resolve
     * @return the matching {@code Verbosity} constant, or {@code null} if the index is out of
     *         bounds
     */
    public static Verbosity getVerbosity(int k)
    {
        if (k < 0 || k >= CACHE.length)
        {
            return null;
        }

        return CACHE[k];
    }
}