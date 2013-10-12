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
public class FailureManager {

    private static final Timer TIMER = new HashedWheelTimer();

    private final Bootstrap clientBootstrap;
    private final String endpoint;
    private final int port;
	private final Timer timer;

	public FailureManager(Bootstrap bootstrap, String endpoint, int port) {
        this(bootstrap, endpoint, port, TIMER);
    }

    public FailureManager(Bootstrap bootstrap, String endpoint, int port, Timer timer) {
        this.clientBootstrap = bootstrap;
        this.endpoint = endpoint;
        this.port = port;
		this.timer = timer;
    }

    public void handleFailure() {
        timer.newTimeout(new TimerTask() {

            @Override
            public void run(Timeout tmt) throws Exception {
                try {
                    clientBootstrap.connect(endpoint, port).addListener(failureHandlingFuture());
                } catch (Exception e) {
                    // what here?
                }
            }
        }, 1, TimeUnit.SECONDS);
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
