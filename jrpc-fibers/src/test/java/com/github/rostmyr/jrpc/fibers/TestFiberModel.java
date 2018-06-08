package com.github.rostmyr.jrpc.fibers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.github.rostmyr.jrpc.fibers.Fiber.*;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class TestFiberModel {
    public Fiber<String> callFiber() {
        String fiberResult = call(fiberWithSeveralCalls());
        return result(fiberResult);
    }

    public Fiber<String> callFuture() {
        String fiberResult = call(getFuture());
        return result(fiberResult);
    }

    public Fiber<Void> getNothing() {
        String fiberResult = call(getFuture());
        return nothing();
    }

    public Fiber<String> callFiberInReturn() {
        String fiberResult = call(getFuture());
        return result(fiberWithSeveralCalls());
    }

    public Fiber<String> callFutureInReturn() {
        return result(getFuture());
    }

    public Fiber<String> callFiberWithArgument() {
        String fiberResult = call(callRegularMethod("A", "B"));
        return result(fiberResult);
    }

    public Fiber<String> callRegularMethod(String second, String third) {
        String first = call(getString());
        return result(join(first, second));
    }

    public Fiber<String> callMethodChain() {
        String string = call(getString().concat("Chained Call!"));
        return result(string);
    }

    public Fiber<String> fiberWithSeveralCalls() {
        String first = call("A");
        String second = call("B");
        return result(join(first, second));
    }

    public Fiber<Integer> fiberWithImmediateReturn() {
        return result(1);
    }

//    public Fiber<String> callFiberTwice() {
//        String fiberResult = call(callRegularMethod(call(callFiber()), "b"));
//        return result(fiberResult);
//    }

//    public Fiber<Integer> loop() {
//        int sum = 0;
//        for (int i = 0; i < 2; i++) {
//            sum += call(i);
//        }
//        return result(sum);
//    }

    public class FiberWithVoidResult extends Fiber<Void> {

        @Override
        public int update() {
            switch (state) {
                case 0: {
                    return 1;
                }
                case 1: {
                    return nothingInternal();
                }
                default: {
                    throw new IllegalStateException("Unknown state: " + state);
                }
            }
        }
    }

    public class CallAnotherFiber extends Fiber<String> {

        @Override
        public int update() {
            switch (state) {
                case 0: {
                    return callInternal(fiberWithSeveralCalls());
                }
                case 1: {
                    this.result = join((String) this.result, "B");
                    return -1;
                }
                default: {
                    throw new IllegalStateException("Unknown state: " + state);
                }
            }
        }
    }

    public class CallFuture extends Fiber<String> {

        @Override
        public int update() {
            switch (state) {
                case 0: {
                    return callInternal(getFuture());
                }
                case 1: {
                    this.result = join((String) this.result, "B");
                    return -1;
                }
                default: {
                    throw new IllegalStateException("Unknown state: " + state);
                }
            }
        }
    }

    public String getString() {
        return "Hello World";
    }

    private Future<String> getFuture() {
        return CompletableFuture.completedFuture("A");
    }

    private static String join(String... string) {
        return String.join("", string);
    }
}
