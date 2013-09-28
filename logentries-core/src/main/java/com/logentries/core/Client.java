package com.logentries.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A Logentries client implementation.
 *
 * <p>The current implementation dispatches events on a single IO thread. Calls
 * to {@link #write(java.lang.String)} are non-blocking.</p>
 *
 * @author Chris Mowforth
 */
@ThreadSafe
public class Client implements IClient {

    /** Logentries API server. */
    public static String ENDPOINT = "localhost";

    private final String token, host, key;
    private final boolean useSSL, useHTTP;
    private final int port;

    private final EventLoopGroup group;
    private final AtomicBoolean opened;

    private final Bootstrap bootstrap;
    private Channel channel;

    private Client(String token, String host, String key, boolean useSSL, boolean useHTTP) {
        this.token = token;
        this.host = host;
        this.key = key;
        this.useSSL = useSSL;
        this.useHTTP = useHTTP;
        this.port = getPort();
        this.group = new NioEventLoopGroup(1);
        this.opened = new AtomicBoolean(false);
        this.bootstrap = new Bootstrap();
    }

    @Override
    public void open() throws Exception {
        Preconditions.checkState(!opened.get()); // Should never have been opened

        final ClientOutboundHandler handler = new ClientOutboundHandler();

        bootstrap.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
         .handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel c) throws Exception {
                c.pipeline().addFirst(new ReconnectHandler(bootstrap, ENDPOINT, port));
                c.pipeline().addFirst(new ClientOutboundHandler());
                if (useHTTP) {
                    c.pipeline().addFirst(new HttpHandler(token, host, key));
                    c.pipeline().addFirst(new HttpRequestEncoder());
                }
            }
        });

        ChannelFuture future = null;
        while (future == null) {
            try {
                future = bootstrap.connect(ENDPOINT, port).sync();
                if (!future.isSuccess()) future = null;
            } catch (Exception ex) {
                Thread.sleep(1000);
                System.out.println("reconnecting...");
            }
        }

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

    private int getPort() {
        if (useHTTP) {
            return useSSL ? 443 : 10000;
        } else {
            return useSSL ? 20000 : 10000;
        }
    }

    public static class Builder {

        private String token, host, key;
        private boolean useSSL, useHTTP;

        public static Builder get() {
            return new Builder();
        }

        private Builder() {
            this.useHTTP = false;
            this.useSSL = true;
        }

        public Builder withToken(@Nonnull String token) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token));
            Preconditions.checkArgument(isValidUUID(token));

            this.token = token;
            return this;
        }

        public Builder withAccountKey(@Nonnull String key) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token));
            Preconditions.checkArgument(isValidUUID(key));

            this.key = key;
            return this;
        }

        public Builder withHostKey(@Nonnull String host) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token));
            Preconditions.checkArgument(isValidUUID(key));

            this.host = host;
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
            Preconditions.checkArgument(!Strings.isNullOrEmpty(token));
            if (useHTTP && key == null) {
                throw new IllegalArgumentException("You must specify an account key to use HTTP");
            }

            return new Client(token, host, key, useSSL, useHTTP);
        }

        private boolean isValidUUID(String uuid) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(uuid));
            UUID u = UUID.fromString(uuid);
            return u.toString().equals(uuid);
        }
    }
}
