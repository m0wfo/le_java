package com.logentries.metrics;

import com.codahale.metrics.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.logentries.core.LogentriesClient;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A periodic reporter which sends
 */
public class LogentriesReporter extends ScheduledReporter {

    private final LogentriesClient client;
    private final ObjectMapper mapper;

    /**
     * Returns a new {@link Builder} for {@link LogentriesReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link LogentriesReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {

        private final MetricRegistry registry;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private Clock clock;
        private MetricFilter filter;
        private LogentriesClient client;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.clock = Clock.defaultClock();
            this.filter = MetricFilter.ALL;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder withClient(LogentriesClient client) {
            this.client = client;
            return this;
        }
        public LogentriesReporter build() {
            Preconditions.checkNotNull(client);
            return new LogentriesReporter(client, registry, rateUnit, durationUnit, filter);
        }
    }

    private LogentriesReporter(
            LogentriesClient client,
            MetricRegistry registry,
            TimeUnit rateUnit,
            TimeUnit durationUnit,
            MetricFilter filter) {
        super(registry, "logentries-reporter", filter, rateUnit, durationUnit);
        this.client = client;
        this.mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.addMixInAnnotations(Sampling.class, PrettyHistogram.class);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        try {
            Map<String, ?> instruments = ImmutableMap.of(
                    "gauges", gauges,
                    "counters", counters,
                    "histograms", histograms,
                    "meters", meters,
                    "timers", timers);
            String s = mapper.writeValueAsString(instruments);
            client.write(s);
            int x = 0;
        } catch (IOException e) {

        }

    }

    @Override
    public void start(long period, TimeUnit unit) {
        super.start(period, unit);
        client.open();
    }

    @Override
    public void stop() {
        super.stop();
        client.close();
    }

    abstract class PrettyHistogram {
        @JsonIgnore
        public abstract Snapshot getSnapshot();
    }
}
