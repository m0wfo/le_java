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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.SSLEngine;

/**
 * A Logentries client implementation.
 *
 * <p>The current implementation dispatches events on a single IO thread. Calls
 * to {@link #write(java.lang.String)} are non-blocking.</p>
 *
 */
@ThreadSafe
public class Client implements IClient {

    /** Logentries API server. */
    public static String ENDPOINT = "localhost";

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

    private Client(int port, String token, String host, String key, boolean useSSL, boolean useHTTP) {
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

    @Override
    public Future open() throws IOException, IllegalStateException {
        Preconditions.checkState(!opened.get()); // Should never have been opened

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel c) throws Exception {
                        c.pipeline().addFirst(new ReconnectHandler(failureManager));
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

    @Override
    public Future write(@Nonnull String message) {
        Preconditions.checkState(opened.get()); // Must be open already
        Preconditions.checkArgument(!Strings.isNullOrEmpty(message), "Cannot write null string to socket.");

        return channel.get().writeAndFlush(message);
    }

    @Override
    public void close() throws IOException, IllegalStateException {
        Preconditions.checkState(opened.get()); // Must be open already
        group.shutdownGracefully();
    }

    /**
     * A builder for creating Logentries clients.
     *
     * <p>{@link Builder} provides a fluid interface for correctly
     * configuring {@link Client}s.</p>
     *
     * <p>Builders can be reused; you can safely call {@link #build()} on a
     * single instance multiple times (although in practice it's rare that
     * you'd need to).</p>
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

        public Client build() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token),
                    "You must specify a log token.");
            Preconditions.checkArgument(!(useHTTP && key == null),
                    "You must specify an account key to use the HTTP input.");
            Preconditions.checkArgument(!(useHTTP && host == null),
                    "You must specify a host key to use the HTTP input.");

            return new Client(getPort(), token, host, key, useSSL, useHTTP);
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
