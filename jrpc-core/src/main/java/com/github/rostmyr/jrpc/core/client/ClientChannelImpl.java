package com.github.rostmyr.jrpc.core.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rostmyr.jrpc.core.client.handler.ClientHandlerInitializer;
import com.github.rostmyr.jrpc.core.client.proxy.ClientProxyFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.rostmyr.jrpc.common.utils.SystemUtils.isLinux;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ClientChannelImpl implements ClientChannel {
    private static final Logger log = LoggerFactory.getLogger(ClientChannelImpl.class);

    private final InetSocketAddress targetAddress;
    private final Channel networkChannel;
    private final ServerResponseListener serverResponseListener;
    private final EventLoopGroup eventLoop = isLinux() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    private final ClientProxyFactory clientProxyFactory;

    public ClientChannelImpl(InetSocketAddress targetAddress) {
        this.targetAddress = targetAddress;
        this.clientProxyFactory = new ClientProxyFactory();
        this.serverResponseListener = new ServerResponseListener(clientProxyFactory.getResourceRegistry());

        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(eventLoop)
            .channel(isLinux() ? EpollSocketChannel.class : NioSocketChannel.class)
            .handler(new ClientHandlerInitializer(serverResponseListener))
            .remoteAddress(targetAddress)
            .option(ChannelOption.SO_KEEPALIVE, true);

        // connect to the remote peer, wait until the connect completes
        try {
            networkChannel = bootstrap.connect().sync().channel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClientChannel shutdown() {
        if (networkChannel.isOpen()) {
            networkChannel.close().addListener(future -> {
                if (!future.isSuccess()) {
                    log.warn("Error during server channel shutdown '{}'", future.cause());
                }
                serverResponseListener.destroy();
                eventLoop.shutdownGracefully().addListener(result -> {
                    if (!result.isSuccess()) {
                        log.warn("Error during server event loop shutdown '{}'", result.cause());
                    }
                });
            });
        }
        return this;
    }

    @Override
    public Channel getUnderlyingChannel() {
        return networkChannel;
    }

    @Override
    public Future<?> newCall(ServerCall serverCall) {
        CompletableFuture<?> listener = serverResponseListener.waitFor(serverCall.getId(), serverCall.getResponseType());
        networkChannel.writeAndFlush(serverCall).addListener(result -> {
            if (!result.isSuccess()) {
                serverResponseListener.setExceptional(serverCall.getId(), result.cause());
                return;
            }
            if (serverCall.isVoid()) {
                serverResponseListener.setResponse(serverCall.getId(), null);
            }
        });
        return listener;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> T createProxy(Class<T> clazz) {
        return clientProxyFactory.create(clazz, this);
    }
}
