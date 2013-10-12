package com.logentries.core.format;

/**
 * Platform and Logentries-specific delimiters used for event formatting.
 *
 * <p>Java client libraries should use these constants in custom formatters.</p>
 *
 */
public final class Delimiters {

    /**
     * Standard newline used to delimit Logentries events.
     */
    public static final String NEWLINE = "\n";
    /**
     * The cross-platform newline char used to signify non-delimiting newlines
     * within an event.
     */
    public static final String LE_NEWLINE = "\u2028";
    /**
     * Character used to signify indentation within an event.
     */
    public static final String INDENT_CHAR = "\t";
}
