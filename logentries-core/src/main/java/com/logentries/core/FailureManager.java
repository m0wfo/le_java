package com.logentries.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import rx.Observable;

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

    /** Limit, in seconds, to stop backing off exponentially. */
    public static int BACKOFF_THRESHOLD = 128;

    private static final Timer TIMER = new HashedWheelTimer();

    private final AtomicReference<Channel> ref;
    private final Bootstrap clientBootstrap;
    private final String endpoint;
    private final int port, limit;
	private final Timer timer;
    private final AtomicInteger timeout;
    private final AtomicReference<Timeout> task;

	FailureManager(AtomicReference<Channel> ref, Bootstrap bootstrap, String endpoint, int port) {
        this(ref, bootstrap, endpoint, port, TIMER, BACKOFF_THRESHOLD);
    }

    FailureManager(AtomicReference<Channel> ref, Bootstrap bootstrap, String endpoint, int port, Timer timer, int limit) {
        this.ref = ref;
        this.clientBootstrap = bootstrap;
        this.endpoint = endpoint;
        this.port = port;
        this.limit = limit;
		this.timer = timer;
        this.timeout = new AtomicInteger();
        this.task = new AtomicReference<Timeout>();
    }

    /**
     * TODO document
     * @return an observable which completes when a connection is established
     */
    public Observable<LogentriesClient> connectReliably() {
        final CountDownLatch latch = new CountDownLatch(1);
        Timeout t = timer.newTimeout(new TimerTask() {

            @Override
            public void run(Timeout tmt) throws Exception {
                try {
                    clientBootstrap.connect(endpoint, port).sync().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture f) throws Exception {
                            if (f.isSuccess()) {
                                ref.set(f.channel());
                                latch.await();
                                timeout.set(0);
                            }
                        }
                    });
                } catch (Exception e) {
                    int delay = timeout.get();
                    // If we've invoked a fresh connection or
                    // hit the back-off threshold, set delay to 1
                    if (delay == 0 || delay >= limit) {
                        delay = 1;
                    } else {
                        delay *= 2;
                    }
                    timeout.set(delay);
                    connectReliably();
                }
            }
        }, timeout.get(), TimeUnit.SECONDS);

        task.set(t);

        return Observable.from(new Future<LogentriesClient>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return task.get().cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.get().isCancelled();
            }

            @Override
            public boolean isDone() {
                return latch.getCount() == 0;
            }

            @Override
            public LogentriesClient get() throws InterruptedException, ExecutionException {
                latch.await();
                return null; // TODO- return a LogentriesClient
            }

            @Override
            public LogentriesClient get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                latch.await(timeout, unit);
                return null; // TODO- return a LogentriesClient
            }
        });
    }

}
