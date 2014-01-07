package com.logentries.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.ssl.SslHandler;

import rx.Observable;
import rx.util.functions.Func1;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import javax.net.ssl.SSLEngine;

/**
 * A client to send event data to Logentries.
 *
 * <p>Before writing any data, the clientmust be opened and closed afterwards.</p>
 *
 * <p>Additionally, the {@link #open()} and {@link #close()} methods have the
 * {@literal @PostConstruct} and {@literal @PreDestroy} annotations to support
 * automated lifecycle management.</p>
 *
 * <p>The current implementation dispatches events on a single IO thread.</p>
 *
 */
@ThreadSafe
public class LogentriesClient implements Closeable {

    /** Logentries API server. */
    public static String ENDPOINT = "data.logentries.com";

	// Config-related members
    private final String token, host, key;
    private final boolean useSSL, useHTTP;
    private final int port;

	// I/O-related members
    private final EventLoopGroup group;
    private final AtomicBoolean opened;
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel;
    private final FailureManager failureManager;

    private LogentriesClient(int port, String token, String host, String key, boolean useSSL, boolean useHTTP) {
        this.token = token;
        this.host = host;
        this.key = key;
        this.useSSL = useSSL;
        this.useHTTP = useHTTP;
        this.port = port;
        this.group = new NioEventLoopGroup(1);
        this.opened = new AtomicBoolean(false);
        this.bootstrap = new Bootstrap();
        this.channel = new AtomicReference<Channel>();
        this.failureManager = new FailureManager(channel, bootstrap, ENDPOINT, port);
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
    public Observable<LogentriesClient> open() throws IllegalStateException {
        Preconditions.checkState(!opened.get()); // Should never have been opened

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel c) throws Exception {
                        c.pipeline().addFirst(new ReconnectHandler(failureManager));
                        c.pipeline().addFirst(new NewlineHandler());
                        c.pipeline().addFirst(new ClientOutboundHandler());
                        if (useHTTP) {
                            c.pipeline().addFirst(new HttpHandler(token, host, key));
                            c.pipeline().addFirst(new HttpRequestEncoder());
                        }
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
        return failureManager.connectReliably();
    }

    private Observable<LogentriesClient> write(String message) {
        final LogentriesClient me = this;
        return Observable.from(this.channel.get().write(message))
                .map(new Func1<Void, LogentriesClient>() {
            @Override
            public LogentriesClient call(Void aVoid) {
                return me;
            }
        });
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
     * @param messages a log message
     */
    public Observable<LogentriesClient> write(@Nonnull String... messages) {
        Preconditions.checkState(opened.get()); // Must be open already
        Observable<LogentriesClient> writeFuture = Observable.from(this);
        for (String message : messages) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(message), "Null or empty string.");
            writeFuture = writeFuture.map()
        }
        return writeFuture;
    }

    /**
     * TODO document
     * @throws IllegalStateException if the client has never been opened for writing.
     */
    @Override @PreDestroy
    public void close() throws IllegalStateException {
        Preconditions.checkState(opened.get()); // Must be open already
        group.shutdownGracefully();
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

        private String token, host, key;
        private boolean useSSL, useHTTP;

        public static Builder get() {
            return new Builder();
        }

        private Builder() {
            this.useHTTP = false;
            this.useSSL = true;
        }

        public Builder withToken(@Nonnull String logToken) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(logToken));
            Preconditions.checkArgument(isValidUUID(logToken));

            this.token = logToken;
            return this;
        }

        public Builder withAccountKey(@Nonnull String accountKey) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(accountKey));
            Preconditions.checkArgument(isValidUUID(accountKey));

            this.key = accountKey;
            return this;
        }

        public Builder withHostKey(@Nonnull String hostKey) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(hostKey));
            Preconditions.checkArgument(isValidUUID(hostKey));

            this.host = hostKey;
            return this;
        }

        public Builder usingSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder usingHTTP(boolean useHTTP) {
            this.useHTTP = useHTTP;
            return this;
        }

        public LogentriesClient build() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token),
                    "You must specify a log token.");
            Preconditions.checkArgument(!(useHTTP && key == null),
                    "You must specify an account key to use the HTTP input.");
            Preconditions.checkArgument(!(useHTTP && host == null),
                    "You must specify a host key to use the HTTP input.");

            return new LogentriesClient(getPort(), token, host, key, useSSL, useHTTP);
        }

        private boolean isValidUUID(String uuid) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(uuid));
            UUID u = UUID.fromString(uuid);
            return u.toString().equals(uuid);
        }

        private int getPort() {
            if (useHTTP) {
                return useSSL ? 443 : 80;
            } else {
                return useSSL ? 20000 : 10000;
            }
        }
    }
}
