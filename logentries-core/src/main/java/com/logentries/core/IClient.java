package com.logentries.core;

import io.netty.channel.ChannelFuture;

import java.io.Closeable;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;

/**
 * A client to dispatch events to Logentries.
 *
 * <p>{@link IClient} implementations are managed objects which extend {@link Closeable}.
 * Before writing any data, they must be opened and closed at the beginning and end of
 * their lifecycle.</p>
 *
 * <p></p>
 *
 * @author chris
 */
public interface IClient extends Closeable {

    /**
     * Starts a client.
     *
     * <p>Called to initialize the client on start-up. Consecutive invocations
     * may put the client into an undefined state.</p>
     *
     * @throws Exception
     */
    public Future open() throws Exception;

    /**
     * Write a string to Logentries.
     *
     * <p>Implementing classes should avoid blocking where possible, but are
     * permitted to drop the guarantee under high load or during loss of network
     * connectivity.</p>
     *
     * <p>TODO- talk about message formatting</p>
     *
     * @param message
     */
    public Future write(@Nonnull String message);
}
