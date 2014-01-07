package com.logentries.metrics;

import com.codahale.metrics.*;
import com.logentries.core.Client;
import com.logentries.core.LogentriesClientI;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LogentriesReporter extends ScheduledReporter {

    private final LogentriesClientI client;

    private LogentriesReporter(MetricRegistry registry,
                               String logToken,
                               TimeUnit rateUnit,
                               TimeUnit durationUnit,
                               MetricFilter filter) {
        super(registry, "logentries-reporter", filter, rateUnit, durationUnit);
        this.client = Client.Builder.get().withToken("").build();
    }
    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        // TODO
    }

}
