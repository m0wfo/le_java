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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
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
    private final FailureManager failureManager;
    private Channel channel;

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
        this.failureManager = new FailureManager(bootstrap, ENDPOINT, port);
    }

	/**
	 * TODO: blocking behaviour?
	 * @throws IOException
	 * @throws IllegalStateException
	 */
    @Override
    public void open() throws IOException, IllegalStateException {
        Preconditions.checkState(!opened.get()); // Should never have been opened

        bootstrap.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
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
        });

        ChannelFuture future;

        try {
            future = bootstrap.connect(ENDPOINT, port)
                    .addListener(failureManager.failureHandlingFuture())
                    .sync()
                    .addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                channel = f.channel();
            }
        });
        } catch (Exception ex) {}

//        ChannelFuture future = null;
//        while (future == null) {
//            try {
//                future = bootstrap.connect(ENDPOINT, port).sync();
//                if (!future.isSuccess()) future = null;
//            } catch (Exception ex) {
//                Thread.sleep(100);
//                System.out.println("reconnecting...");
//            }
//        }

        opened.set(true);
    }

    @Override
    public void write(@Nonnull String message) {
        Preconditions.checkState(opened.get()); // Must be open already
        Preconditions.checkArgument(!Strings.isNullOrEmpty(message));

        channel.writeAndFlush(message);
    }

    @Override
    public void close() throws IOException, IllegalStateException {
        Preconditions.checkState(opened.get()); // Must be open already
        group.shutdownGracefully();
    }

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
