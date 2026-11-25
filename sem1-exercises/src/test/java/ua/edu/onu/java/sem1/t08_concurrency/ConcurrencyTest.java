package ua.edu.onu.java.sem1.t08_concurrency;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    @Test
    void safeCounterDoesNotLoseIncrements() throws InterruptedException {
        SafeCounter counter = new SafeCounter();
        int threadCount = 8;
        int perThread = 5_000;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    counter.increment();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(threadCount * perThread, counter.get());
    }

    @Test
    void parallelSumComputesExactTotal() {
        List<int[]> chunks = List.of(
                new int[]{1, 2, 3, 4},
                new int[]{10, 20, 30},
                new int[]{100, 200}
        );
        assertEquals(370L, ParallelSumTask.sumInParallel(chunks, 3));
    }
}
