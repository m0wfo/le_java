package com.logentries.log4j;

import com.google.common.base.Throwables;
import com.logentries.core.LogentriesClient;
import com.logentries.core.format.Formatters;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Logentries appender for log4j.
 *
 * @author Mark Lacomber
 */
public final class LogentriesAppender extends AppenderSkeleton {

    private final LogentriesClient.Builder builder;

    private LogentriesClient client;

    LogentriesAppender(LogentriesClient c) {
        builder = LogentriesClient.Builder.get();
        client = c;
    }

    public LogentriesAppender()
    {
        builder = LogentriesClient.Builder.get();
    }

    /**
     * Sets the token.
     *
     * @param token
     */
    public void setToken( String token) {
        builder.withToken(token);
    }

    /**
     * Sets the HTTP PUT boolean flag. Send logs via HTTP instead of default Token TCP
     *
     * @param httpPut HttpPut flag to set
     */
    public void setHttpPut(boolean httpPut) {
        builder.usingHTTP(httpPut);
    }

    /**
     * Sets the ACCOUNT KEY value for HTTP PUT.
     *
     * @param accountKey
     */
    @Deprecated
    public void setKey(String accountKey)
    {
        builder.withAccountKey(accountKey);
    }

    /**
     * Sets the LOCATION value for HTTP PUT.
     *
     * @param logLocation
     */
    @Deprecated
    public void setLocation(String logLocation)
    {
        // no-op
    }

    /**
     * Sets the SSL boolean flag.
     *
     * @param ssl
     */
    public void setSsl(boolean ssl)
    {
        builder.usingSSL(ssl);
    }

    @Override
    public void activateOptions() {
        try {
            client = builder.build();
            client.open();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Implements AppenderSkeleton Append method, handles time and format.
     *
     * @event event to log
     */
    @Override
    protected void append(LoggingEvent event) {

        // Render the event according to layout
        String formattedEvent = layout.format( event);

        // Append stack trace if present
        String[] stack = event.getThrowableStrRep();
        if (stack != null) {
            formattedEvent += ", ";
            formattedEvent += Formatters.joinWithUnicodeLineBreak(stack);
        }

        // Write log event
        client.write(formattedEvent);
    }

    /**
     * Closes all connections to Logentries.
     */
    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            String trace = Throwables.getStackTraceAsString(e);
            errorHandler.error("Unable to close Logentries client: " + trace);
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
