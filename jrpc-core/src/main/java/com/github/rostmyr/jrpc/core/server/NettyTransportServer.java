package com.github.rostmyr.jrpc.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.common.utils.SystemUtils;
import com.github.rostmyr.jrpc.core.exception.ServerBindException;
import com.github.rostmyr.jrpc.core.server.handler.TransportServerChannelInitializer;
import com.github.rostmyr.jrpc.core.server.registry.ImmutableResourceRegistry;
import com.github.rostmyr.jrpc.core.server.registry.MutableResourceRegistry;
import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.ServerServiceDefinition;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Rostyslav Myroshnychenko
 * on 21.05.2018.
 */
class NettyTransportServer implements TransportServer {
    private static final Logger log = LoggerFactory.getLogger(NettyTransportServer.class);

    private final InetSocketAddress address;
    private final List<ServerServiceDefinition> serviceDefinitions;
    private final ResourceRegistry resourceRegistry = new MutableResourceRegistry();

    private final boolean isLinux = SystemUtils.isLinux();
    private final MultithreadEventLoopGroup eventLoopGroup = isLinux ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private TransportServerListener listener;
    private Channel channel;

    NettyTransportServer(InetSocketAddress address, List<ServerServiceDefinition> serviceDefinitions) {
        this.address = address;
        this.serviceDefinitions = unmodifiableList(serviceDefinitions);

        for (ServerServiceDefinition serviceDefinition : this.serviceDefinitions) {
            for (MethodDefinition methodDefinition : serviceDefinition.getMethods()) {
                resourceRegistry.add(methodDefinition.getInputResourceId(), methodDefinition.getInputType());
            }
        }
    }

    public void start(TransportServerListener listener) throws ServerBindException {
        this.listener = Contract.checkNotNull(listener, "listener can't be null");

        TransportServerChannelInitializer channelInitializer = new TransportServerChannelInitializer(
            serviceDefinitions, new ImmutableResourceRegistry(resourceRegistry)
        );

        ServerBootstrap sb = new ServerBootstrap();
        sb.group(eventLoopGroup)
            .channel(isLinux ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            .childHandler(channelInitializer)
            .localAddress(address);

        ChannelFuture future = sb.bind(address);
        try {
            future.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for bind");
        }
        if (!future.isSuccess()) {
            throw new ServerBindException("Failed to bind a server at port: " + getPort(), future.cause());
        }
        channel = future.channel();
        listener.onStarted();
    }

    public void shutdown() {
        if (channel == null || !channel.isOpen()) {
            return;
        }
        channel.close().addListener(future -> {
            if (!future.isSuccess()) {
                log.warn("Error during server channel shutdown '{}'", future.cause());
            }
            eventLoopGroup.shutdownGracefully().addListener(result -> {
                if (!result.isSuccess()) {
                    log.warn("Error during server event loop shutdown '{}'", result.cause());
                }
                listener.onShutdown();
            });
        });
    }

    public int getPort() {
        return address.getPort();
    }
}
