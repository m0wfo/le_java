package com.logentries.log4j;

import com.google.common.base.Throwables;
import com.logentries.core.Client;
import com.logentries.core.IClient;
import com.logentries.core.format.Formatters;
import java.io.IOException;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Logentries appender for log4j.
 *
 * @author Mark Lacomber
 *
 */
public class LogentriesAppender extends AppenderSkeleton {

    private final Client.Builder builder;

    private IClient client;

    LogentriesAppender(IClient c) {
        builder = Client.Builder.get();
        client = c;
    }
    public LogentriesAppender()
    {
        builder = Client.Builder.get();
    }

    /*
     * Public methods to send log4j parameters to AsyncLogger
     */
    /**
     * Sets the token
     *
     * @param token
     */
    public void setToken( String token) {
        builder.withToken(token);
    }

    /**
     *  Sets the HTTP PUT boolean flag. Send logs via HTTP PUT instead of default Token TCP
     *
     *  @param httpput HttpPut flag to set
     */
    public void setHttpPut( boolean HttpPut) {
        builder.usingHTTP(HttpPut);
    }

    /** Sets the ACCOUNT KEY value for HTTP PUT
     *
     * @param account_key
     */
    public void setKey( String account_key)
    {
        builder.withAccountKey(account_key);
    }

    /**
     * Sets the LOCATION value for HTTP PUT
     *
     * @param log_location
     */
    @Deprecated
    public void setLocation( String log_location)
    {
        // no-op
    }

    /**
     * Sets the SSL boolean flag
     *
     * @param ssl
     */
    public void setSsl( boolean ssl)
    {
        builder.usingSSL(ssl);
    }

    @Override
    public void activateOptions() {
        try {
            client = builder.build();
            client.open();
        } catch (Exception e) {
            errorHandler.error("Unable to start Logentries client: " + e);
        }
    }

    /**
     * Implements AppenderSkeleton Append method, handles time and format
     *
     * @event event to log
     */
    @Override
    protected void append( LoggingEvent event) {

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
     * Closes all connections to Logentries
     */
    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            String trace = Throwables.getStackTraceAsString(e);
            errorHandler.error("Unable to close Logentries client: " + trace);
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
