package com.github.rostmyr.jrpc.core.server;

import com.github.rostmyr.jrpc.core.exception.ServerBindException;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public interface TransportServer {
    /**
     * Starts a server
     */
    void start(TransportServerListener listener) throws ServerBindException;

    /**
     * Initiates an shutdown of the server
     */
    void shutdown();

    /**
     * Returns the underlying port
     */
    int getPort();
}
