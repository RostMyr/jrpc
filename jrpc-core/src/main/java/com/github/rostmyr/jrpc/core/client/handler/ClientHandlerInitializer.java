package com.github.rostmyr.jrpc.core.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import com.github.rostmyr.jrpc.core.client.ServerResponseListener;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ClientHandlerInitializer extends ChannelInitializer {
    private final ServerResponseListener responseListener;

    public ClientHandlerInitializer(ServerResponseListener serverResponseListener) {
        this.responseListener = serverResponseListener;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline().addLast(new ServerResponseDecoder(responseListener));
        ch.pipeline().addLast(new ServerCallEncoder());
    }
}
