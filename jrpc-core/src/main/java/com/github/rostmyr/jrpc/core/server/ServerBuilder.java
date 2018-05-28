package com.github.rostmyr.jrpc.core.server;


import com.github.rostmyr.jrpc.core.service.ServerServiceDefinition;
import com.github.rostmyr.jrpc.core.service.reader.ServiceDefinitionReader;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Rostyslav Myroshnychenko
 * on 21.05.2018.
 */
public class ServerBuilder {
    private final InetSocketAddress address;
    private final List<ServerServiceDefinition> serviceDefinitions = new ArrayList<>();
    private final ServiceDefinitionReader serviceDefinitionReader = new ServiceDefinitionReader();
    // add stats recorder

    private ServerBuilder(InetSocketAddress address) {
        this.address = address;
    }

    private ServerBuilder(int port) {
        this.address = new InetSocketAddress(port);
    }

    public static ServerBuilder forPort(int port) {
        return new ServerBuilder(port);
    }

    public static ServerBuilder forAddress(InetSocketAddress address) {
        return new ServerBuilder(address);
    }

    public ServerBuilder addService(Object service) {
        serviceDefinitions.add(serviceDefinitionReader.read(service));
        return this;
    }

    public ServerBuilder addServices(Object... services) {
        for (Object service : services) {
            addService(service);
        }
        return this;
    }

    public Server build() {
        return new TcpServer(new NettyTransportServer(address, serviceDefinitions));
    }
}
