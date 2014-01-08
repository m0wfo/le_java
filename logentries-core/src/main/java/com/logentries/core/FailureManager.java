package com.logentries.core;

import com.google.common.base.Throwables;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.net.ConnectException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to manage connection failures and retry policy.
 *
 * <p>Instead of connecting directly through a {@link Bootstrap},
 * a {@link FailureManager} handles connect exceptions and
 * asynchronous retry attempts, moving connection failure logic
 * out of {@link LogentriesClient} and handler code.</p>
 */
class FailureManager {

    /**
     * Limit, in seconds, to stop backing off exponentially.
     */
    public static int BACKOFF_THRESHOLD = 128;

    private final AtomicReference<Channel> ref;
    private final Bootstrap clientBootstrap;
    private final String endpoint;
    private final int port, limit;
    private final Timer timer;
    private final AtomicInteger timeout;

    FailureManager(AtomicReference<Channel> ref, Bootstrap bootstrap, String endpoint, int port) {
        this(ref, bootstrap, endpoint, port, new HashedWheelTimer(), BACKOFF_THRESHOLD);
    }

    FailureManager(AtomicReference<Channel> ref, Bootstrap bootstrap, String endpoint, int port, Timer timer, int limit) {
        this.ref = ref;
        this.clientBootstrap = bootstrap;
        this.endpoint = endpoint;
        this.port = port;
        this.limit = limit;
        this.timer = timer;
        this.timeout = new AtomicInteger();
    }

    /**
     * TODO document
     *
     * @return an observable which completes when a connection is established
     */
    public Future connectReliably() {
        CountDownLatch latch = new CountDownLatch(1);
        return connectReliably(new ConnectFuture(latch));
    }

    private Future connectReliably(final ConnectFuture future) {
        Timeout t = timer.newTimeout(new TimerTask() {

            @Override
            public void run(Timeout tmt) throws Exception {
                try {
                    clientBootstrap.connect(endpoint, port).sync().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture f) throws Exception {
                            if (f.isSuccess()) {
                                ref.set(f.channel());
                                timeout.set(0);
                                future.getLatch().countDown();
                            }
                        }
                    });
                } catch (Exception e) {
                    if (!(e instanceof ConnectException || e instanceof InterruptedException)) {
                        Throwables.propagate(e);
                    }

                    int delay = timeout.get();
                    // If we've invoked a fresh connection or
                    // hit the back-off threshold, set delay to 1
                    if (delay == 0 || delay >= limit) {
                        delay = 1;
                    } else {
                        // Otherwise back-off exponentially
                        // TODO- we should back off linearly
                        // for 30s or so first
                        delay *= 2;
                    }
                    timeout.set(delay);
                    try {
                        connectReliably(future);
                    } catch (IllegalStateException ex) {
                        String s = "";
                    }
                }
            }
        }, timeout.get(), TimeUnit.SECONDS);

        future.setTimeout(t);
        return future;
    }

    public void stop() {
        timer.stop();
    }

    private static class ConnectFuture implements Future {

        private final CountDownLatch latch;
        private Timeout task;

        public ConnectFuture(CountDownLatch latch) {
            this.latch = latch;
        }

        public void setTimeout(Timeout timeout) {
            this.task = timeout;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return latch.getCount() == 0;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            latch.await();
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            latch.await(timeout, unit);
            return null;
        }
    }
}