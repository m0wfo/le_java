package com.logentries.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.SSLEngine;
import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client to send event data to Logentries.
 *
 * <p>Before writing any data, the clientmust be opened and closed afterwards.</p>
 *
 * <p>Additionally, the {@link #open()} and {@link #close()} methods have the
 * {@literal @PostConstruct} and {@literal @PreDestroy} annotations to support
 * automated lifecycle management.</p>
 */
@ThreadSafe
public class LogentriesClient implements Closeable {

    /**
     * Logentries API endpoint.
     */
    public static String ENDPOINT = "api.logentries.com";
    /**
     * Logentries JS API endpoint.
     */
    public static String JS_ENDPOINT = "js.logentries.com";

    // Config-related members
    private final UUID token;
    private final String apiHost;
    private final boolean useSSL, useHTTP;
    private final int apiPort;

    // I/O-related members
    private final EventLoopGroup group;
    private final AtomicBoolean opened;
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel;
    private final FailureManager failureManager;

    private LogentriesClient(int port, String host, UUID token, boolean useSSL, boolean useHTTP) {
        this.token = token;
        this.apiPort = port;
        this.apiHost = host;
        this.useSSL = useSSL;
        this.useHTTP = useHTTP;

        this.group = new NioEventLoopGroup();
        this.opened = new AtomicBoolean(false);
        this.bootstrap = new Bootstrap();
        this.channel = new AtomicReference<Channel>();
        this.failureManager = new FailureManager(channel, bootstrap, apiHost, apiPort);
    }

    /**
     * Starts a client.
     *
     * <p>Called to initialize the client on start-up. Consecutive invocations
     * are not allowed.</p>
     *
     * @throws IllegalStateException if the client is already open.
     */
    @PostConstruct
    public void open() throws IllegalStateException {
        Preconditions.checkState(!opened.get()); // Should never have been opened

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel c) throws Exception {
                        if (useHTTP) {
                            c.pipeline().addFirst(new HttpHandler(token));
                            c.pipeline().addFirst(new HttpRequestEncoder());
                        } else {
                            c.pipeline().addFirst(new SimpleEntryHandler(token));
                        }
                        c.pipeline().addFirst(new ReconnectHandler(failureManager));
                        if (useSSL) {
                            SSLEngine engine = SSLContextProvider.getContext().createSSLEngine();
                            engine.setUseClientMode(true);
                            c.pipeline().addFirst(new SslHandler(engine));
                        }
                    }
                })
                .validate();

        opened.set(true);

        // Connect through the failure manager
        try {
            failureManager.connectReliably().get();
        } catch (InterruptedException ex) {
            Throwables.propagate(ex);
        } catch (ExecutionException ex) {
            Throwables.propagate(ex);
        }
    }

    /**
     * Write a string to Logentries.
     *
     * <p>Implementing classes should avoid blocking where possible, but are
     * permitted to drop the guarantee under high load or during loss of network
     * connectivity. TODO- reword this.</p>
     *
     * <p>If {@link #write(String[])} is called with a string that isn't terminated
     * in a newline delimiter, the client will add one to ensure it appears properly
     * in the log dashboard.</p>
     *
     * <p>Thread safety: multiple threads can call {@link #write(String...)} on
     * a single instance concurrently.</p>
     *
     * @param messages a log message(s)
     */
    public Future write(@Nonnull String... messages) throws IllegalStateException {
        Preconditions.checkState(opened.get()); // Must be open already
        Future future = Futures.immediateFuture(null);
        for (String message : messages) {
            future = channel.get().write(message);
        }

        return future;
    }

    /**
     * Close the client instance.
     *
     * @throws IllegalStateException if the client was never opened.
     */
    @Override
    @PreDestroy
    public void close() throws IllegalStateException {
        Preconditions.checkState(opened.get()); // Must be open already
        try {
            group.shutdownGracefully().await();
        } catch (InterruptedException e) {
        }
        failureManager.stop();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for creating Logentries clients.
     *
     * <p>{@link Builder} provides a fluid interface for correctly
     * configuring {@link LogentriesClient}s.</p>
     *
     * <p>Thread safety: Builder instance methods should not be called by
     * multiple threads concurrently.</p>
     */
    @NotThreadSafe
    public static final class Builder {

        private UUID token;
        private boolean useSSL, useHTTP;

        private Builder() {
            this.useHTTP = false;
            this.useSSL = true;
        }

        /**
         * Sets the log token for the client instance.
         *
         * @param logToken string representation of a log token
         * @return this {@link Builder} with assigned token
         */
        public Builder withToken(@Nonnull UUID logToken) {
            this.token = logToken;
            return this;
        }

        /**
         * Enable or disable transport encryption.
         *
         * <p><strong>Optional. Defaults to {@literal true}.</strong></p>
         *
         * @param useSSL
         * @return this {@link Builder} instance
         */
        public Builder usingSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        /**
         * TODO document
         *
         * @param useHTTP
         * @return
         */
        public Builder usingHTTP(boolean useHTTP) {
            this.useHTTP = useHTTP;
            return this;
        }

        public LogentriesClient build() {
            Preconditions.checkArgument(token != null,
                    "You must specify a log token.");

            return new LogentriesClient(getPort(), getEndpoint(), token, useSSL, useHTTP);
        }

        private int getPort() {
            if (useHTTP) {
                return useSSL ? 443 : 80;
            } else {
                return useSSL ? 20000 : 10000;
            }
        }

        private String getEndpoint() {
            return useHTTP ? JS_ENDPOINT : ENDPOINT;
        }
    }
}
