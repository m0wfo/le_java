package com.logentries.logback;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogentriesAppenderTest {

    private static final Logger log = LoggerFactory
            .getLogger(LogentriesAppenderTest.class);

    private static final String token = "some-token";
    private static final String location = "some location";
    private static final String accountKey = "account key";
    private static final String facility = "DAEMON";

    @Test
    public void testSomething() {
    }
}

