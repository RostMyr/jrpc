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

    // the current fiber we are waiting for
    protected Fiber current;
    protected FiberManager scheduler;

    public void setState(int state) {
        this.state = state;
    }

    public void awaitFor(Fiber fiber) {
        this.current = fiber;
        if (current.scheduler == null) {
            scheduler.schedule(current);
        }
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
     * case 0:
     *    awaitFor(call(fiber))
     *    return 1
     * case 1:
     *    return callInternal(); // returns 1 while current.isReady() returns false
     * case 2:
     *    this.someVariable = this.result;
     * ...
     */
    protected int callInternal() {
        if (!current.isReady()) {
            return state;
        }
        this.result = current.result;
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
    public static <T, F extends Fiber<T>> F result(F fiber) {
        return null;
    }

    /**
     * Should be called by {@link #result(Fiber)}}
     *
     * ...
     * case n:
     *    awaitFor(call(fiber))
     *    return n + 1
     * case n + 1:
     *    return resultInternal(); // returns n + 1 while current.isReady() returns false
     * ...
     */
    protected int resultInternal() {
        if (!current.isReady()) {
            return state;
        }
        this.result = current.result;
        return -1;
    }

    /**
     * A marker method
     */
    public static <T, F extends Future<T>> Fiber<T> result(F call) {
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
