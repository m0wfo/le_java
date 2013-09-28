package com.logentries.core;

import com.lmax.disruptor.EventFactory;

/**
 *
 * @author chris
 */
public final class LogEvent {

    private String data;

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public static EventFactory<LogEvent> FACTORY = new EventFactory<LogEvent>() {

        @Override
        public LogEvent newInstance() {
            return new LogEvent();
        }
    };
}
