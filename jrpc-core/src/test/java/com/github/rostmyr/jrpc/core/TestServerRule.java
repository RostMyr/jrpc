package com.github.rostmyr.jrpc.core;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.github.rostmyr.jrpc.core.server.Server;
import com.github.rostmyr.jrpc.core.server.ServerBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Represents a JUnit {@link org.junit.rules.TestRule} that starts a server.
 * It is useful for mocking out external services and asserting that the expected requests were made.
 *
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class TestServerRule extends ExternalResource {
    private final int port;
    private Server server;

    public TestServerRule(int port, Object... services) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
            .addServices(services)
            .build();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @Override
    protected void before() throws Throwable {
        server.start();
    }

    @Override
    protected void after() {
        server.shutdown();
        try {
            server.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            server = null;
        }
    }

    public int getPort() {
        return port;
    }
}
