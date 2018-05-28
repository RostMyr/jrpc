package com.github.rostmyr.jrpc.core.server;

import com.github.rostmyr.jrpc.core.exception.ServerBindException;

import java.util.concurrent.TimeUnit;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public interface Server {
    /**
     * Starts a server
     */
    Server start() throws ServerBindException;

    /**
     * Initiates an shutdown of the server
     */
    Server shutdown();

    /**
     * Checks whether the server is shutdown.
     * Shutdown servers reject any new calls, but may still have some calls being processed.
     */
    boolean isShutdown();

    /**
     * Returns whether the server is terminated.
     * Terminated servers have no running calls.
     */
    boolean isTerminated();

    /**
     * Waits for the server to become terminated, giving up if the timeout is reached
     *
     * @return whether the server is terminated, as would be done by {@link #isTerminated()}.
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for the server to become terminated
     */
    void awaitTermination() throws InterruptedException;

    /**
     * Returns the underlying port
     */
    int getPort();
}
