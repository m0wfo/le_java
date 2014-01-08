package com.logentries.logback;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static com.logentries.core.format.Delimiters.LE_NEWLINE;
import static com.logentries.core.format.Delimiters.INDENT_CHAR;

public class ExceptionFormatterTest {

    @Test
    public void testTopLevelTrace() {
        IThrowableProxy error = Mockito.mock(IThrowableProxy.class);

        StackTraceElementProxy l1 = Mockito.mock(StackTraceElementProxy.class);
        Mockito.stub(l1.getSTEAsString()).toReturn("trace level 1");

        StackTraceElementProxy[] topLevel = new StackTraceElementProxy[]{l1};

        Mockito.stub(error.getStackTraceElementProxyArray()).toReturn(topLevel);
        Mockito.stub(error.getClassName()).toReturn("com.foo.SomeClass");
        Mockito.stub(error.getMessage()).toReturn("err!");

        String trace = ExceptionFormatter.formatException(error);
        Assert.assertEquals(trace, "com.foo.SomeClass: err!" +
                LE_NEWLINE + INDENT_CHAR + "trace level 1" + LE_NEWLINE);
    }
}