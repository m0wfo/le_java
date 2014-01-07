package com.logentries.log4j;

import static org.junit.Assert.assertEquals;

import com.logentries.core.LogentriesClient;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LogentriesAppenderTest {

    private LogentriesClient client;
    private LogentriesAppender appender;
    private Logger logger;

    @Before
    public void setup() {
        client = Mockito.mock(LogentriesClient.class);
        appender = new LogentriesAppender(client);
        logger = Logger.getLogger(LogentriesAppenderTest.class);
        logger.addAppender(appender);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testThrowsWithNoConfiguration() {
        appender.activateOptions();
    }

    @Test
    public void testSomething() {
        logger.debug("message");

    }
}
