package com.logentries.core.format;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 */
@ThreadSafe
public class Formatters {

    public static String joinWithUnicodeLineBreak(String[] lines) {
        Joiner joiner = Joiner.on(Delimiters.LE_NEWLINE);
        return joiner.join(lines);
    }

	/**
	 * Appends a newline character to the end of a string if there isn't one
	 * present.
	 *
	 * <p>If the final character of the input string is a newline, this method
	 * will return the original string. If not, it will add a {@link Delimiters.NEWLINE}
	 * onto the end.</p>
	 *
	 * @param input the input string
	 * @return a newline-terminated string
	 */
    public static String appendNewlineIfNeeded(@Nonnull String input) {
		Preconditions.checkState(!Strings.isNullOrEmpty(input));

        if (!input.substring(input.length() - 1).equals(Delimiters.NEWLINE)) {
            return input + Delimiters.NEWLINE;
        }
        return input;
    }
}
