package com.logentries.core.format;

import junit.framework.Assert;
import org.junit.Test;

public class FormattersTest {

	/**
	 * TODO: extend joiner tests
	 */
	@Test
	public void testJoinWithUnicodeLineBreak() {
		String[] input = new String[] {};
		String out = Formatters.joinWithUnicodeLineBreak(input);
		Assert.assertEquals(out, "");
	}

	/**
	 * Appending a newline onto an empty string should fail.
	 */
	@Test(expected=IllegalStateException.class)
	public void testAppendToEmptyStringFails() {
		Formatters.appendNewlineIfNeeded("");
	}

	/**
	 * Appending a newline onto a null string should fail.
	 */
	@Test(expected = IllegalStateException.class)
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
