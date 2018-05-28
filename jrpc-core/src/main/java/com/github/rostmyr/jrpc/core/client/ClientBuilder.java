package com.github.rostmyr.jrpc.core.client;

import java.net.InetSocketAddress;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
public class ClientBuilder {
    private final InetSocketAddress serverAddress;

    private ClientBuilder(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    private ClientBuilder(int port) {
        this.serverAddress = new InetSocketAddress(port);
    }

    public static ClientBuilder forPort(int port) {
        return new ClientBuilder(port);
    }

    public static ClientBuilder forAddress(InetSocketAddress address) {
        return new ClientBuilder(address);
    }

    public ClientChannel build() {
        return new ClientChannelImpl(serverAddress);
    }
}
