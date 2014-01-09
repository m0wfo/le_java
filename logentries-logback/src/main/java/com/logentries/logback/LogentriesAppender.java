package com.logentries.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogConstants;
import com.logentries.core.LogentriesClient;

import java.util.UUID;

/**
 * Logentries appender for logback.
 *
 * @author Mark Lacomber
 * @author Ben McCann
 */
public class LogentriesAppender extends AppenderBase<ILoggingEvent> {

    private final LogentriesClient.Builder builder;

    /**
     * Asynchronous Background logger
     */
    private LogentriesClient client;
    /**
     * Layout
     */
    private Layout<ILoggingEvent> layout;
    /**
     * Facility String
     */
    private String facilityStr;
    /**
     * Default Suffix Pattern
     */
    static final public String DEFAULT_SUFFIX_PATTERN = "[%thread] %logger %msg";

    protected String suffixPattern;

    /**
     * Initializes asynchronous logging.
     */
    public LogentriesAppender() {
        builder = LogentriesClient.builder();
    }

	/*
     * Public methods to send logback parameters to AsyncLogger
	 */

    /**
     * Sets the log token.
     *
     * <p>Specifies the token UUID corresponding to the
     * Logentries log you want to send events to.</p>
     *
     * @param token log token
     */
    public void setToken(String token) {
        builder.withToken(UUID.fromString(token));
    }

    /**
     * Sets the HTTP boolean flag. Send logs via HTTP instead of default Token TCP.
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
    public void setKey(String accountKey) {
        // no-op
    }

    /**
     * Sets the LOCATION value for HTTP PUT
     *
     * @param log_location
     */
    @Deprecated
    public void setLocation(String log_location) {
        // no-op
    }

    /**
     * Sets the SSL boolean flag
     *
     * @param ssl
     */
    public void setSsl(boolean ssl) {
        builder.usingSSL(ssl);
    }

    /**
     * Sets the debug flag. Appender in debug mode will print error messages on
     * error console.
     *
     * @param debug debug flag to set
     */
    @Deprecated
    public void setDebug(boolean debug) {
        // no-op
    }

    @Override
    public void start() {
        if (layout == null) {
            layout = buildLayout();
        }
        client = builder.build();
        client.open();
        super.start();
    }

    String getPrefixPattern() {
        return "%syslogStart{" + getFacility() + "}%nopex";
    }

    /**
     * Returns the string value of the <b>Facility</b> option.
     *
     * See {@link #setFacility} for the set of allowed values.
     */
    public String getFacility() {
        return facilityStr;
    }

    /**
     * The <b>Facility</b> option must be set one of the strings KERN, USER, MAIL,
     * DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, NTP, AUDIT,
     * ALERT, CLOCK, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5, LOCAL6,
     * LOCAL7. Case is not important.
     *
     * <p>
     * See {@link SyslogConstants} and RFC 3164 for more information about the
     * <b>Facility</b> option.
     */
    public void setFacility(String facilityStr) {
        if (facilityStr != null) {
            facilityStr = facilityStr.trim();
        }
        this.facilityStr = facilityStr;
    }

    /**
     * Sets the layout for the Appender
     */
    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    /**
     * Implements AppenderSkeleton Append method, handles time and format
     *
     * @event event to log
     */
    @Override
    protected void append(ILoggingEvent event) {

        // Render the event according to layout
        String formattedEvent = layout.doLayout(event);

        // Append stack trace if present
        IThrowableProxy error = event.getThrowableProxy();
        if (error != null) {
            formattedEvent += ExceptionFormatter.formatException(error);
        }

        client.write(formattedEvent);
    }

    /**
     * Closes all connections to Logentries
     */
    @Override
    public void stop() {
        super.stop();
        client.close();
    }

    public Layout<ILoggingEvent> buildLayout() {
        PatternLayout l = new PatternLayout();
        l.getInstanceConverterMap().put("syslogStart", SyslogStartConverter.class.getName());
        if (suffixPattern == null) {
            suffixPattern = DEFAULT_SUFFIX_PATTERN;
        }
        l.setPattern(getPrefixPattern() + suffixPattern);
        l.setContext(getContext());
        l.start();
        return l;
    }

    /**
     * See {@link #setSuffixPattern(String).
     *
     * @return
     */
    public String getSuffixPattern() {
        return suffixPattern;
    }

    /**
     * The <b>suffixPattern</b> option specifies the format of the
     * non-standardized part of the message sent to the syslog server.
     *
     * @param suffixPattern
     */
    public void setSuffixPattern(String suffixPattern) {
        this.suffixPattern = suffixPattern;
    }
}
