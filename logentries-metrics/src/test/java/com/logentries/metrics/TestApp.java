package com.logentries.metrics;

import com.codahale.metrics.*;
import com.logentries.core.LogentriesClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * CHANGEME
 */
public class TestApp {

    public static void main(String[] args) {
        MetricRegistry metrics = new MetricRegistry();
        Counter c = metrics.counter("my_first_counter");
        LogentriesClient.Builder builder = LogentriesClient.builder();
        LogentriesClient client = builder
                .withToken(UUID.fromString("13a65478-4fae-42bb-8ce0-a01dd11d2509"))
                .build();
        LogentriesReporter reporter = LogentriesReporter
                .forRegistry(metrics)
                .withClient(client)
                .build();

        Histogram histogram = metrics.histogram("a_histogram");
        Meter m = metrics.meter("a_meter");
        for (int i = 0; i < 1000; i++) {
            histogram.update((long)Math.random());
            m.mark(10);
        }
        Timer t = metrics.timer("a_timer");
        Timer.Context ctx = t.time();
        ctx.stop();
//        client.open();
        reporter.start(1, TimeUnit.SECONDS);
        c.inc();
        try {
            Thread.sleep(10000);
        } catch (Exception e) {}
    }
}
