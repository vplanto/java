package org.example;

/**
 * Небезпечна версія Банки без синхронізації.
 * Швидка, але призводить до стану гонитви (Race Condition) та втрати даних.
 */
class UnsafeDonationJar {
    private long totalDonations = 0;

    public void donate(long amount) {
        totalDonations += amount;
    }

    public long getTotalDonations() {
        return totalDonations;
    }
}

/**
 * Потокобезпечна версія Банки з ключовим словом synchronized.
 * Гарантує взаємне виключення (Mutual Exclusion) та збереження кожної гривні.
 */
class SafeDonationJar {
    private long totalDonations = 0;

    public synchronized void donate(long amount) {
        totalDonations += amount;
    }

    public synchronized long getTotalDonations() {
        return totalDonations;
    }
}

public class DonationJarSample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  БЕНЧМАРК: ПОРІВНЯННЯ СИНХРОНІЗОВАНОГО ТА НЕСИНХРОНІЗОВАНОГО КОДУ");
        System.out.println("======================================================================\n");

        runSimulationBenchmark(2000, 10);
        runContentionBenchmark(10, 100_000, 1);
    }

    /**
     * Тест 1: 2000 окремих потоків (як у симуляторі Main.java)
     */
    private static void runSimulationBenchmark(int numberOfDonators, long amount) throws InterruptedException {
        long expectedTotal = numberOfDonators * amount;
        System.out.println(">>> ТЕСТ 1: СИМУЛЯТОР МАСОВОГО ЗБОРУ (" + numberOfDonators + " потоків по 1 донату)");

        // 1. Unsafe
        UnsafeDonationJar unsafeJar = new UnsafeDonationJar();
        Thread[] unsafeThreads = new Thread[numberOfDonators];
        for (int i = 0; i < numberOfDonators; i++) {
            unsafeThreads[i] = new Thread(() -> unsafeJar.donate(amount));
        }

        long startUnsafe = System.currentTimeMillis();
        for (Thread t : unsafeThreads) t.start();
        for (Thread t : unsafeThreads) t.join();
        long durationUnsafe = System.currentTimeMillis() - startUnsafe;

        // 2. Safe
        SafeDonationJar safeJar = new SafeDonationJar();
        Thread[] safeThreads = new Thread[numberOfDonators];
        for (int i = 0; i < numberOfDonators; i++) {
            safeThreads[i] = new Thread(() -> safeJar.donate(amount));
        }

        long startSafe = System.currentTimeMillis();
        for (Thread t : safeThreads) t.start();
        for (Thread t : safeThreads) t.join();
        long durationSafe = System.currentTimeMillis() - startSafe;

        System.out.printf("  [Unsafe] Зібрано: %d грн (втрачено: %d грн) | Час: %d мс%n",
                unsafeJar.getTotalDonations(), (expectedTotal - unsafeJar.getTotalDonations()), durationUnsafe);
        System.out.printf("  [Safe]   Зібрано: %d грн (втрачено: %d грн) | Час: %d мс%n%n",
                safeJar.getTotalDonations(), (expectedTotal - safeJar.getTotalDonations()), durationSafe);
    }

    /**
     * Тест 2: Висока інтенсивність (10 постійних потоків по 100 000 операцій)
     * Показує чисту вартість синхронізації без накладних витрат на створення 2000 OS-потоків.
     */
    private static void runContentionBenchmark(int threadsCount, int opsPerThread, long amount) throws InterruptedException {
        long expectedTotal = (long) threadsCount * opsPerThread * amount;
        System.out.println(">>> ТЕСТ 2: ЧИСТА ЦІНА СИНХРОНІЗАЦІЇ (" + threadsCount + " потоків x " + opsPerThread + " операцій = " + expectedTotal + ")");

        // 1. Unsafe
        UnsafeDonationJar unsafeJar = new UnsafeDonationJar();
        Thread[] unsafeThreads = new Thread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            unsafeThreads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    unsafeJar.donate(amount);
                }
            });
        }

        long startUnsafe = System.nanoTime();
        for (Thread t : unsafeThreads) t.start();
        for (Thread t : unsafeThreads) t.join();
        long durationUnsafeMs = (System.nanoTime() - startUnsafe) / 1_000_000;

        // 2. Safe
        SafeDonationJar safeJar = new SafeDonationJar();
        Thread[] safeThreads = new Thread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            safeThreads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    safeJar.donate(amount);
                }
            });
        }

        long startSafe = System.nanoTime();
        for (Thread t : safeThreads) t.start();
        for (Thread t : safeThreads) t.join();
        long durationSafeMs = (System.nanoTime() - startSafe) / 1_000_000;

        System.out.printf("  [Unsafe] Зібрано: %d (втрачено: %d) | Час: %d мс%n",
                unsafeJar.getTotalDonations(), (expectedTotal - unsafeJar.getTotalDonations()), durationUnsafeMs);
        System.out.printf("  [Safe]   Зібрано: %d (втрачено: %d) | Час: %d мс%n",
                safeJar.getTotalDonations(), (expectedTotal - safeJar.getTotalDonations()), durationSafeMs);

        if (durationSafeMs > durationUnsafeMs) {
            double slowdown = (double) durationSafeMs / Math.max(1, durationUnsafeMs);
            System.out.printf("  -> Синхронізація захистила 100%% даних, але зайняла в ~%.1f рази більше часу через черги на моніторі!%n", slowdown);
        }
        System.out.println("======================================================================\n");
    }
}
