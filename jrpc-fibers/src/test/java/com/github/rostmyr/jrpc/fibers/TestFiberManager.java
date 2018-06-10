package com.github.rostmyr.jrpc.fibers;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rostyslav Myroshnychenko
 * on 31.05.2018.
 */
public class TestFiberManager {


    @Test
    public void shouldAddNewFibers() {
        // GIVEN
        FiberManager manager = FiberManagers.current();

        // WHEN
        for (int i = 0; i < 10; i++) {
            manager.schedule(new MyFiber());
        }

        // THEN
        assertThat(manager.getSize()).isEqualTo(10);

        manager.run();
    }

    private static class MyFiber extends Fiber<Integer> {
        private static int ID_SEQ = 1;

        private final int id = ID_SEQ++;

        @Override
        public int update() {
            switch (state) {
                case 0:
                    return ThreadLocalRandom.current().nextInt(0, 2);
                case 1:
                    return 2;
                case 2:
                    result = ThreadLocalRandom.current().nextInt();
                    return -1;
            }
            throw new IllegalStateException();
        }

        @Override
        public String toString() {
            return "MyFiber{" +
                "id=" + id +
                '}';
        }
    }

}