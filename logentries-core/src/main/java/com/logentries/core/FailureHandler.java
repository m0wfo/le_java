package com.logentries.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author chris
 */
public class FailureHandler {

    private static final Timer TIMER = new HashedWheelTimer();
    private final Bootstrap clientBootstrap;
    private final String endpoint;
    private final int port;

    public FailureHandler(Bootstrap bootstrap, String endpoint, int port) {
        this.clientBootstrap = bootstrap;
        this.endpoint = endpoint;
        this.port = port;
    }

    public void handleFailure() {
        TIMER.newTimeout(new TimerTask() {

            @Override
            public void run(Timeout tmt) throws Exception {
                clientBootstrap.connect(endpoint, port).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (!f.isSuccess()) {
                            System.out.println("err");
                            handleFailure();
                        }
                    }
                });
            }
        }, 10, TimeUnit.MILLISECONDS);
    }

    public ChannelFutureListener failureHandlingFuture() {
        return new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (!f.isSuccess()) {
                    handleFailure();
                }
            }
        };
    }
}
