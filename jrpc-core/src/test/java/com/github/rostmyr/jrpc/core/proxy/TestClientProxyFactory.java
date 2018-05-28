package com.github.rostmyr.jrpc.core.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.client.ServerCall;
import com.github.rostmyr.jrpc.core.client.proxy.ClientProxyFactory;
import com.github.rostmyr.jrpc.core.resource.EmptyResource;
import com.github.rostmyr.jrpc.core.service.ServiceA;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestClientProxyFactory {
    private final ClientProxyFactory proxyFactory = new ClientProxyFactory();

    @Test
    public void shouldCreateClientProxy() {
        // GIVEN
        ClientChannel clientChannel = mock(ClientChannel.class);
        Channel channel = mock(Channel.class);
        when(clientChannel.getUnderlyingChannel()).thenReturn(channel);
        when(clientChannel.newCall(any(ServerCall.class))).thenReturn(CompletableFuture.completedFuture(null));
        ByteBufAllocator allocator = mock(ByteBufAllocator.class);
        when(allocator.buffer()).thenReturn(Unpooled.buffer());
        when(channel.alloc()).thenReturn(allocator);

        // WHEN
        ServiceA proxy = proxyFactory.create(ServiceA.class, clientChannel);

        // THEN
        assertThat(proxy).isNotNull();
        proxy.doSomethingWithResource(new EmptyResource());
        verify(clientChannel).newCall(any(ServerCall.class));
    }


    public static class NullResource implements Resource {
        public static final int _resourceId = 0;

        public static Supplier<NullResource> create() {
            return NullResource::new;
        }

        @Override
        public int getResourceId() {
            return 0;
        }

        @Override
        public void read(ByteBuf in) {

        }

        @Override
        public void write(ByteBuf out) {

        }
    }
}