package com.logentries.core.format;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helpers for converting log data into Logentries-compatible
 * formatting.
 */
@ThreadSafe
public final class Formatters {

	private static final CharMatcher MATCHER = CharMatcher.JAVA_LETTER_OR_DIGIT
			.or(CharMatcher.anyOf("._"));
	private static final Escaper ESCAPER = Escapers.builder()
			.addEscape('\'', "\\'")
			.build();

    public static String joinWithUnicodeLineBreak(@Nonnull String[] lines) {
        Preconditions.checkArgument(lines != null);

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
		Preconditions.checkArgument(!Strings.isNullOrEmpty(input));

        if (!input.substring(input.length() - 1).equals(Delimiters.NEWLINE)) {
            return input + Delimiters.NEWLINE;
        }
        return input;
    }

	/**
	 * TODO document
	 * @param input
	 * @return
	 */
	public static String replaceNewlinesWithUnicode(@Nonnull String input) {
		return CharMatcher.anyOf(Delimiters.NEWLINE)
				.replaceFrom(input, Delimiters.LE_NEWLINE);
	}

	/**
	 * TODO document
	 * @param input
	 * @return
	 */
	public static String formatLogEntry(@Nonnull String input) {
		String newlinesReplaced = replaceNewlinesWithUnicode(input);
		return appendNewlineIfNeeded(newlinesReplaced);
	}

	private static String stringify(Object obj) {
		return stringify(obj, "", 0);
	}

	private static String stringify(Object obj, String parent, int depth) {
		if (obj == null) return "null";
		if (!isComplex(obj)) {
			return escape(obj.toString());
		} else {
			if (obj instanceof List) {
				List l = (List) obj;
				List<String> t = new LinkedList<String>();
				for (int i = 0; i < l.size(); i++) {
					String val = stringify(l.get(i), parent, depth + 1);
					if (Strings.isNullOrEmpty(parent)) {
						t.add(val);
					} else {
						if (depth > 0) {
							t.add(parent + "." + String.valueOf(i) + "." + String.valueOf(depth) + "=" + val);
						} else
							t.add(parent + "." + String.valueOf(i) + "=" + val);
					}
				}
				return Joiner.on(" ").join(t);
			} else if (obj instanceof Map) {
				Map<String, Object> m = (Map) obj;
				List<String> t = new LinkedList<String>();
				for (String k : m.keySet()) {
					String level;
					if (Strings.isNullOrEmpty(parent)) {
						level = k;
					} else {
						level = parent + "." + k;
					}
					String val = stringify(m.get(k), level, depth);
					if (m.get(k) instanceof Map || m.get(k) instanceof List) {
						t.add(val);
					} else {
						t.add(level + "=" + val);
					}
				}
				return Joiner.on(" ").join(t);
			}
		}
		return "";
	}

	private static boolean isComplex(Object obj) {
		return (obj instanceof Map || obj instanceof List);
	}

	private static String escape(String str) {
		if (!MATCHER.matchesAllOf(str)) {
			return "'" + ESCAPER.escape(str) + "'";
		} else {
			return str;
		}
	}
}