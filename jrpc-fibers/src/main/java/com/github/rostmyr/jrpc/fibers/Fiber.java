package com.github.rostmyr.jrpc.fibers;

import java.util.concurrent.Future;

/**
 * Rostyslav Myroshnychenko
 * on 31.05.2018.
 */
public abstract class Fiber<E> {
    protected Object result;
    protected Throwable exception;
    protected int state;
    protected Fiber<?> next;
    protected FiberManager scheduler;

    public void setState(int state) {
        this.state = state;
    }

    public E getResult() {
        return (E) result;
    }

    public abstract int update();

    /**
     * A marker method
     */
    public static <T> T call(T call) {
        return call;
    }

    /**
     * A marker method
     */
    public static <T> T call(Fiber<T> call) {
        return null;
    }

    /**
     * A marker method
     */
    public static <T, F extends Future<T>> T call(F call) {
        return null;
    }

    /**
     * A marker method
     */
    public static <T> Fiber<T> result(T result) {
        return null;
    }

    public boolean isReady() {
        return state == -1;
    }
}
