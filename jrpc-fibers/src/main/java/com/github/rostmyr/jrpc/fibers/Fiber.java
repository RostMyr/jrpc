package com.github.rostmyr.jrpc.fibers;

import java.util.concurrent.ExecutionException;
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
     * Should be called by {@link #call(Fiber)}}
     *
     *
     * case 0:
     *    return this.call(this, fiber) // returns 0 while fiber.isReady() returns false
     * case 1:
     *    this.someVariable = this.result;
     *    return 2;
     * ...
     */
    protected <T> int callInternal(Fiber<T> callee) {
        if (callee.scheduler == null) {
            scheduler.schedule(callee);
        }
        if (!callee.isReady()) {
            return state;
        }
        this.result = callee.result;
        return state + 1;
    }

    /**
     * A marker method
     */
    public static <T, F extends Future<T>> T call(F call) {
        return null;
    }

    /**
     * Should be called by {@link #call(Future)}}
     *
     *
     * case 0:
     *    return this.call(this, future) // returns 0 while future.isDone() returns false
     * case 1:
     *    this.someVariable = this.result;
     *    return 2;
     * ...
     */
    protected <T, F extends Future<T>> int callInternal(F future) {
        if (!future.isDone()) {
            return state;
        }
        try {
            this.result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            this.exception = e;
            return -1;
        }
        return state + 1;
    }

    /**
     * A marker method
     */
    public static <T> Fiber<T> result(T result) {
        return null;
    }

    /**
     * A marker method
     */
    public static <T, F extends Fiber<T>> T result(F fiber) {
        return null;
    }

    /**
     * Should be called by {@link #result(Fiber)}}
     *
     *
     * case 0:
     *    return this.call(this, fiber) // returns 0 while fiber.isDone() returns false
     * case 1:
     *    this.someVariable = this.result;
     *    return 2;
     * ...
     */
    protected <T> int resultInternal(Fiber<T> callee) {
        if (!callee.isReady()) {
            return state;
        }
        this.result = callee.result;
        return -1;
    }

    /**
     * A marker method
     */
    public static <T, F extends Future<T>> T result(F call) {
        return null;
    }

    /**
     * Should be called by {@link #result(Fiber)}}
     *
     *
     * case 0:
     *    return this.call(this, future) // returns 0 while future.isDone() returns false
     * case 1:
     *    this.someVariable = this.result;
     *    return 2;
     * ...
     *
     */
    protected <T, F extends Future<T>> int resultInternal(F future) {
        if (!future.isDone()) {
            return state;
        }
        try {
            this.result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            this.exception = e;
        }
        return -1;
    }

    /**
     * A marker method
     */
    public static Fiber<Void> nothing() {
        return null;
    }

    /**
     * Should be called by {@link #nothing()}}
     *
     * ...
     * case n:
     *    return nothingInternal();
     * ...
     *
     */
    protected int nothingInternal() {
        return -1;
    }

    public boolean isReady() {
        return state == -1;
    }
}
