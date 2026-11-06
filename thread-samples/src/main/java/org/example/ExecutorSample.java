package org.example;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Навчальний стенд до Практикуму 8 (p08_executors.md):
 * 1. Порівняння накладних витрат new Thread().start() проти перевикористання пулу потоків ExecutorService.
 * 2. Паралельне опитування складів із поверненням результату через Callable<Integer> та Future<Integer>.
 * 3. Неблокуючий асинхронний конвеєр на CompletableFuture.
 */
public class ExecutorSample {

    static class AidCalculator implements Callable<Integer> {
        private final String warehouseName;

        public AidCalculator(String warehouseName) {
            this.warehouseName = warehouseName;
        }

        @Override
        public Integer call() throws Exception {
            System.out.printf("  [%s] %s: починаю підрахунок...%n",
                    Thread.currentThread().getName(), warehouseName);
            int processingTimeMs = ThreadLocalRandom.current().nextInt(200, 600);
            Thread.sleep(processingTimeMs);

            int itemsNeeded = ThreadLocalRandom.current().nextInt(100, 1000);
            System.out.printf("  [%s] %s: потрібно %d од. (час: %d мс)%n",
                    Thread.currentThread().getName(), warehouseName, itemsNeeded, processingTimeMs);
            return itemsNeeded;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println("  ПРАКТИКУМ 8: EXECUTORSERVICE, CALLABLE, FUTURE ТА COMPLETABLEFUTURE");
        System.out.println("======================================================================\n");

        runThreadCreationVsPoolBenchmark(2000);
        runLogisticsCenterCallableDemo();
        runCompletableFuturePipelineDemo();
    }

    /**
     * Частина 1: Чому new Thread().start() на кожен запит програє пулу потоків.
     */
    private static void runThreadCreationVsPoolBenchmark(int tasksCount) throws InterruptedException {
        System.out.println(">>> 1. БЕНЧМАРК: " + tasksCount + " ЗАДАЧ (new Thread() vs FixedThreadPool(10))");

        // Підхід 1: ручне створення системного потоку на кожну задачу
        AtomicInteger manualCounter = new AtomicInteger();
        Thread[] threads = new Thread[tasksCount];
        long startManual = System.nanoTime();
        for (int i = 0; i < tasksCount; i++) {
            threads[i] = new Thread(manualCounter::incrementAndGet);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long manualDurationMs = (System.nanoTime() - startManual) / 1_000_000;

        // Підхід 2: фіксований пул із 10 потоків
        AtomicInteger poolCounter = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        long startPool = System.nanoTime();
        for (int i = 0; i < tasksCount; i++) {
            pool.submit(poolCounter::incrementAndGet);
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        long poolDurationMs = (System.nanoTime() - startPool) / 1_000_000;

        System.out.printf("  [new Thread() x %d]      Виконано: %d | Час: %d мс%n",
                tasksCount, manualCounter.get(), manualDurationMs);
        System.out.printf("  [FixedThreadPool(10)]     Виконано: %d | Час: %d мс%n%n",
                 poolCounter.get(), poolDurationMs);
    }

    /**
     * Частина 2: Callable<Integer> та Future<Integer> — паралельне опитування складів.
     */
    private static void runLogisticsCenterCallableDemo() throws Exception {
        System.out.println(">>> 2. CALLABLE + FUTURE: ПАРАЛЕЛЬНЕ ОПИТУВАННЯ СКЛАДІВ");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        long start = System.currentTimeMillis();
        Future<Integer> futureKyiv = executor.submit(new AidCalculator("Склад (Київ)"));
        Future<Integer> futureLviv = executor.submit(new AidCalculator("Склад (Львів)"));
        Future<Integer> futureKharkiv = executor.submit(new AidCalculator("Склад (Харків)"));

        int totalNeeded = futureKyiv.get() + futureLviv.get() + futureKharkiv.get();
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("  -> Загальна потреба з 3 складів: %d од. (сумарний час паралельного опитування: %d мс)%n%n",
                totalNeeded, elapsed);

        executor.shutdown();
    }

    /**
     * Частина 3: CompletableFuture — неблокуючий конвеєр обробки замовлення.
     */
    private static void runCompletableFuturePipelineDemo() {
        System.out.println(">>> 3. COMPLETABLEFUTURE: АСИНХРОННИЙ КОНВЕЄР");

        String finalReport = CompletableFuture.supplyAsync(() -> {
                    System.out.printf("  [%s] Крок 1: завантаження даних зі складу...%n",
                            Thread.currentThread().getName());
                    return 450;
                })
                .thenApply(boxes -> {
                    System.out.printf("  [%s] Крок 2: перерахунок у палети (%d коробок)...%n",
                            Thread.currentThread().getName(), boxes);
                    return (int) Math.ceil(boxes / 30.0);
                })
                .thenApply(pallets -> String.format("Готово до відправки: %d палет", pallets))
                .join();

        System.out.println("  -> Результат конвеєра: " + finalReport);
        System.out.println("======================================================================");
    }
}

