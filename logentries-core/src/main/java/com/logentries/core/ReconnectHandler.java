package com.logentries.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Handles connection retry behaviour.
 * @author chris
 */
public class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private static Timer TIMER = new HashedWheelTimer();

    private final Bootstrap clientBootstrap;
    private final String endpoint;
    private final int port;

    public ReconnectHandler(Bootstrap bootstrap, String endpoint, int port) {
        this.clientBootstrap = bootstrap;
        this.endpoint = endpoint;
        this.port = port;
    }

    /**
     * Callback invoked when connection is lost.
     * @param ctx channel context
     * @throws Exception
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        TIMER.newTimeout(new TimerTask() {

            @Override
            public void run(Timeout tmt) throws Exception {
                clientBootstrap.connect(endpoint, port).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (!f.isSuccess()) {
                            channelInactive(ctx);
                        }
                    }
                });
            }
        }, 10, TimeUnit.SECONDS);
    }
}
