package com.logentries.metrics;

import com.codahale.metrics.*;
import com.logentries.core.LogentriesClient;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LogentriesReporter extends ScheduledReporter {

    private final LogentriesClient client;

    private LogentriesReporter(
            LogentriesClient client,
            MetricRegistry registry,
            TimeUnit rateUnit,
            TimeUnit durationUnit,
            MetricFilter filter) {
        super(registry, "logentries-reporter", filter, rateUnit, durationUnit);
        this.client = client;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        // TODO
    }

}
