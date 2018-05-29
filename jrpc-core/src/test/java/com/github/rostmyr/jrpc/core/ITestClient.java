package com.github.rostmyr.jrpc.core;

import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.client.ClientBuilder;
import com.github.rostmyr.jrpc.core.client.ClientChannel;
import com.github.rostmyr.jrpc.core.client.proxy.ClientProxyFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class ITestClient {
    private static final ServerMock mock = Mockito.mock(ServerMock.class);

    @ClassRule
    public static final TestServerRule SERVER = new TestServerRule(4040, new ServiceAImpl(mock));

    private final ClientChannel clientChannel = ClientBuilder.forPort(SERVER.getPort()).build();

    private final ClientProxyFactory proxyFactory = new ClientProxyFactory();

    @Before
    public void init() {
        reset(mock);
    }

    @Test
    public void shouldCallVoidMethod() {
        // GIVEN
        ServiceA serviceProxy = proxyFactory.create(ServiceA.class, clientChannel);

        // WHEN
        serviceProxy.consumeResource(new MyResource());

        // THEN
        verify(mock).call();
    }

    @Test
    public void shouldCallMethodWithResponse() {
        // GIVEN
        ServiceA serviceProxy = proxyFactory.create(ServiceA.class, clientChannel);

        // WHEN
        int response = serviceProxy.returnSomething(new MyResource());

        // THEN
        assertThat(response).isEqualTo(5);
    }

    @Test
    @Ignore
    public void measureMethodWithResponse() {
        // GIVEN
        ServiceA serviceProxy = proxyFactory.create(ServiceA.class, clientChannel);

        // WHEN
        int result = 0;
        MyResource resource = new MyResource();
        long now = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            result += serviceProxy.returnSomething(resource);
        }
        long passed = System.nanoTime() - now;

        System.out.println("Executed in '" + TimeUnit.NANOSECONDS.toMillis(passed) + " ' ms");

        assertThat(result).isEqualTo(1_000_000 * 5);
    }
    //Executed in '32850 ' ms

    public interface ServiceA {
        void consumeResource(MyResource resource);

        int returnSomething(MyResource resource);
    }

    public static class ServiceAImpl implements ServiceA {

        private final ServerMock mock;

        public ServiceAImpl(ServerMock mock) {
            this.mock = mock;
        }

        @Override
        public void consumeResource(MyResource resource) {
            System.out.println(resource);
            mock.call();
        }

        @Override
        public int returnSomething(MyResource resource) {
            return 5;
        }

    }

    public static class MyResource implements Resource {
        public static final int _resourceId = 10;

        public static Supplier<MyResource> create() {
            return MyResource::new;
        }

        @Override
        public int getResourceId() {
            return 10;
        }

        @Override
        public void read(ByteBuf in) {

        }

        @Override
        public void write(ByteBuf out) {

        }
    }

    private static class ServerMock {
        void call() {

        }
    }

}
