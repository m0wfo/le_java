package com.logentries.core.format;

import junit.framework.Assert;
import org.junit.Test;

import static com.logentries.core.format.Delimiters.LE_NEWLINE;

public class FormattersTest {

	/**
	 * Empty string list should return empty string.
	 */
	@Test
	public void testJoinEmptyListWithUnicodeLineBreak() {
		String[] input = new String[] {};
		String out = Formatters.joinWithUnicodeLineBreak(input);
		Assert.assertEquals(out, "");
	}

    /**
     * Test behaviour of joining string array.
     */
    @Test
    public void testJoinMultipleStrings() {
        String[] input = new String[] {"foo", "bar", "baz"};
        String out = Formatters.joinWithUnicodeLineBreak(input);
        Assert.assertEquals("foo" + LE_NEWLINE + "bar" + LE_NEWLINE + "baz", out);
    }

    /**
     * Joining null string arrays should fail.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullArrayFails() {
        Formatters.joinWithUnicodeLineBreak(null);
    }

	/**
	 * Appending a newline onto an empty string should fail.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testAppendToEmptyStringFails() {
		Formatters.appendNewlineIfNeeded("");
	}

	/**
	 * Appending a newline onto a null string should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testAppendToNullStringFails() {
		Formatters.appendNewlineIfNeeded(null);
	}

	/**
	 * Strings that are already newline-terminated should be left alone.
	 */
	@Test
	public void testConditionalNewlineAppend() {
		String a = "foo";
		String b = "bar\n";

		Assert.assertEquals(Formatters.appendNewlineIfNeeded(a), a + "\n");
		Assert.assertEquals(Formatters.appendNewlineIfNeeded(b), b);
	}
}
