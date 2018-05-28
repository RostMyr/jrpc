package com.github.rostmyr.jrpc.core.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import com.github.rostmyr.jrpc.core.client.handler.ClientHandlerInitializer;

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
    private final InetSocketAddress targetAddress;
    private final Channel networkChannel;
    private final ServerResponseListener serverResponseListener = new ServerResponseListener();

    public ClientChannelImpl(InetSocketAddress targetAddress) {
        this.targetAddress = targetAddress;

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(isLinux() ? new EpollEventLoopGroup() : new NioEventLoopGroup())
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
        return null;
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
}
