package com.github.rostmyr.jrpc.fibers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.github.rostmyr.jrpc.fibers.Fiber.call;
import static com.github.rostmyr.jrpc.fibers.Fiber.result;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class TestFiberModel {

    public Fiber<String> callFiber() {
        String fiberResult = call(fiberWithSeveralCalls());
        return result(fiberResult);
    }

//    public Fiber<String> callFiberTwice() {
//        String fiberResult = call(callRegularMethod(call(callFiber()), "b"));
//        return result(fiberResult);
//    }

    public Fiber<String> callFiberWithArgument() {
        String fiberResult = call(callRegularMethod("A", "B"));
        return result(fiberResult);
    }

    public Fiber<String> callRegularMethod(String second, String third) {
        String first = call(getFirst());
        return result(join(first, second));
    }

    public Fiber<Integer> callMethodChain() {
        String first = call(getFirst().concat("Chained Call!"));
        return result(1);
    }

    public Fiber<String> fiberWithSeveralCalls() {
        String first = call("A");
        String second = call("B");
        String third = call("C");
        return result(first + second + third);
    }

    public Fiber<Integer> fiberWithImmediateReturn() {
        return result(1);
    }

    public static String join(String one, String another) {
        return String.join(one, another);
    }

    public String getFirst() {
        return "Hello";
    }


//    public Fiber<Integer> loop() {
//        int sum = 0;
//        for (int i = 0; i < 2; i++) {
//            sum += call(i);
//        }
//        return result(sum);
//    }

    public class MyFiber extends Fiber<String> {
        private String first;
        private String second;

        public MyFiber(String second) {
            this.second = second;
        }

        private Future<String> getFuture(String A) {
            return null;
        }

        @Override
        public int update() {
            switch (state) {
                case 0: {
                    this.result = getFuture("A");
                    return 1;
                }
                case 1: {
                    Future<String> future = (Future<String>) this.result;
                    if (!future.isDone()) {
                        return 1;
                    }
                    try {
                        this.first = future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    return 2;
                }
                case 2: {
                    this.result = join(first, second);
                    return -1;
                }
                default: {
                    throw new IllegalStateException("Unknown state: " + state);
                }
            }
        }
    }
}
