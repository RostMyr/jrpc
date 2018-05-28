package com.github.rostmyr.jrpc.core.server;

/**
 * Listens for {@link TransportServer} events.
 *
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public interface TransportServerListener {
    /**
     * Transport server is ready to accept incoming connections
     */
    void onStarted();

    /**
     * Transport server is shutdown
     */
    void onShutdown();
}
