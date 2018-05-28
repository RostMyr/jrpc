package com.github.rostmyr.jrpc.core.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;
import com.github.rostmyr.jrpc.core.service.ServerServiceDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * Rostyslav Myroshnychenko
 * on 23.05.2018.
 */
public class TransportServerChannelInitializer extends ChannelInitializer {
    private final Map<String, ServerServiceDefinition> serviceDefinitionsByAddress;
    private final ResourceRegistry resourceRegistry;

    public TransportServerChannelInitializer(
        List<ServerServiceDefinition> serviceDefinitions,
        ResourceRegistry resourceRegistry
    ) {
        this.serviceDefinitionsByAddress = serviceDefinitions.stream()
            .collect(collectingAndThen(toMap(ServerServiceDefinition::getName, identity()), Collections::unmodifiableMap));
        this.resourceRegistry = resourceRegistry;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new TransportServerEncoder());
        pipeline.addLast(new TransportServerDecoder(resourceRegistry, serviceDefinitionsByAddress));
    }
}
