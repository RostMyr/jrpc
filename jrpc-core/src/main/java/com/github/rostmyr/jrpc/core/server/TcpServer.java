package com.github.rostmyr.jrpc.core.server;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.core.exception.ServerBindException;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Rostyslav Myroshnychenko
 * on 22.05.2018.
 */
public class TcpServer implements Server {
    private final TransportServer transportServer;

    private boolean isStarted;
    private boolean isShutdown;
    private boolean isTerminated;

    private final Object lock = new Object();

    TcpServer(TransportServer transportServer) {
        this.transportServer = transportServer;
    }

    @Override
    public Server start() throws ServerBindException {
        synchronized (lock) {
            Contract.checkState(!isStarted, "Server is already running");
            Contract.checkState(!isShutdown, "Server is shutting down");
            transportServer.start(new TransportServerListenerImpl());
            isStarted = true;
        }
        return this;
    }

    @Override
    public Server shutdown() {
        boolean shutdownTransportServer;
        synchronized (lock) {
            if (isShutdown) {
                return this;
            }
            isShutdown = true;
            shutdownTransportServer = isStarted;
            if (!shutdownTransportServer) {
                //  transportServerTerminated = true;
                //  checkForTermination();
            }
        }
        if (shutdownTransportServer) {
            transportServer.shutdown();
        }
        return this;
    }

    @Override
    public boolean isShutdown() {
        synchronized (lock) {
            return isShutdown;
        }
    }

    @Override
    public boolean isTerminated() {
        synchronized (lock) {
            return isTerminated;
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (lock) {
            long timeoutNanos = unit.toNanos(timeout);
            long endTimeNanos = System.nanoTime() + timeoutNanos;
            while (!isTerminated && (timeoutNanos = endTimeNanos - System.nanoTime()) > 0) {
                NANOSECONDS.timedWait(lock, timeoutNanos);
            }
            return isTerminated;
        }
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        synchronized (lock) {
            while (!isTerminated) {
                lock.wait();
            }
        }
    }

    @Override
    public int getPort() {
        return transportServer.getPort();
    }

    private final class TransportServerListenerImpl implements TransportServerListener {
        @Override
        public void onStarted() {

        }

        @Override
        public void onShutdown() {
            synchronized (lock) {
                isTerminated = true;
                lock.notifyAll();
            }
        }
    }
}
