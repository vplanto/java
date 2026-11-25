package ua.edu.onu.java.sem1.t08_concurrency;

import java.util.List;

/**
 * T08 (частина 2): Паралельне виконання задач через ExecutorService та Future.
 */
public final class ParallelSumTask {

    private ParallelSumTask() {
    }

    /**
     * Обчислює суму всіх чисел у списку чанків паралельно за допомогою фіксованого пулу потоків (threads >= 1).
     * Пул потоків має коректно завершуватися (shutdown / try-with-resources).
     */
    public static long sumInParallel(List<int[]> chunks, int threads) {
        throw new UnsupportedOperationException("TODO: реалізувати через Executors.newFixedThreadPool та Future");
    }
}
