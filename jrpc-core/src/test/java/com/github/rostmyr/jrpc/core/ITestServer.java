package com.github.rostmyr.jrpc.core;

import org.junit.Test;

import com.github.rostmyr.jrpc.core.exception.ServerBindException;
import com.github.rostmyr.jrpc.core.server.Server;
import com.github.rostmyr.jrpc.core.server.ServerBuilder;
import com.github.rostmyr.jrpc.core.service.SimpleServiceA;

import java.util.concurrent.TimeUnit;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
public class ITestServer {

    @Test
    public void shouldStartServer() throws ServerBindException, InterruptedException {
        // WHEN-THEN
        Server server = ServerBuilder.forPort(4040)
            .addService(new SimpleServiceA())
            .build();

        server.start();
        server.shutdown();
        server.awaitTermination(1, TimeUnit.MINUTES);
    }
}
