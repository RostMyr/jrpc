package com.github.rostmyr.jrpc.core.client;

import io.netty.channel.Channel;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public interface ClientChannel {
    /**
     * Initiates a shutdown
     *
     * @return this
     */
    ClientChannel shutdown();

    /**
     * Retrieves transport channel
     *
     * @return a channel
     */
    Channel getUnderlyingChannel();


    Future<?> newCall(ServerCall serverCall);

    /**
     * Returns whether the channel is shutdown. Shutdown channels immediately cancel any new calls,
     * but may still have some calls being processed
     *
     * @see #shutdown()
     */
    boolean isShutdown();

    /**
     * Returns whether the channel is already terminated (it has not running requests)
     *
     * @see #isShutdown()
     */
    boolean isTerminated();

    /**
     * Waits for the channel to become terminated, giving up if the timeout is reached.
     *
     * @return {@code true} if channel is terminated, otherwise {@code false}
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> T createProxy(Class<T> clazz);
}
