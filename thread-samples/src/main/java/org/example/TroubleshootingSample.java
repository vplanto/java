package org.example;

/**
 * Практичний стенд до Лекції 7 (Troubleshooting):
 * 1) Демонстрація багатошарового Stack Trace із Caused by (LIFO).
 * 2) Живий процес для зняття Thread Dump через `jcmd` та `jstack <pid>`,
 *    який одночасно відтворює:
 *    - Lock Contention ("вузьке місце": `TIMED_WAITING` всередині `synchronized` + `BLOCKED`),
 *    - Справжній взаємний `DEADLOCK` між двома потоками.
 */
public class TroubleshootingSample {

    private static final Object SHARED_MONITOR = new Object();
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) throws InterruptedException {
        long pid = ProcessHandle.current().pid();
        System.out.println("======================================================================");
        System.out.println("  СТЕНД ДІАГНОСТИКИ JVM (Лекція 7: Troubleshooting)");
        System.out.println("  PID цього Java-процесу: " + pid);
        System.out.println("======================================================================");
        System.out.println("1) Приклад вкладеного Stack Trace (читаємо останній Caused by):");
        printSampleChainedStackTrace();

        System.out.println("\n2) Запускаємо іменовані потоки для аналізу через Thread Dump...");
        startLockContentionScenario();
        startDeadlockScenario();

        System.out.println("\nВідкрийте термінал і виконайте:");
        System.out.println("  jcmd");
        System.out.println("  jstack " + pid);
        System.out.println("\n(Процес працюватиме 10 хвилин або до зупинки Ctrl+C / Stop в IDE)");

        Thread.sleep(10 * 60 * 1000L);
    }

    private static void printSampleChainedStackTrace() {
        try {
            initializeOrderModule();
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
        }
    }

    private static void initializeOrderModule() {
        try {
            connectToDatabase("jdbc:postgresql://localhost:5432/orders");
        } catch (IllegalStateException e) {
            throw new RuntimeException("Не вдалося запустити сервіс замовлень", e);
        }
    }

    private static void connectToDatabase(String url) {
        openTcpSocket(url);
    }

    private static void openTcpSocket(String url) {
        throw new IllegalStateException("Connection refused: БД недоступна за адресою " + url);
    }

    /**
     * Приклад 1 із лекції: "вузьке місце" (Lock Contention) — це НЕ дедлок!
     * Потік "blocking-holder" спить усередині synchronized, а "blocked-waiter" чекає на монітор.
     */
    private static void startLockContentionScenario() throws InterruptedException {
        Thread holder = new Thread(() -> {
            synchronized (SHARED_MONITOR) {
                try {
                    Thread.sleep(10 * 60 * 1000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "blocking-holder");

        Thread waiter = new Thread(() -> {
            synchronized (SHARED_MONITOR) {
                System.out.println("Цей рядок виконається лише після звільнення SHARED_MONITOR");
            }
        }, "blocked-waiter");

        holder.setDaemon(true);
        waiter.setDaemon(true);
        holder.start();
        Thread.sleep(100);
        waiter.start();
    }

    /**
     * Приклад 2 із лекції: справжній взаємний клинч (Java-level DEADLOCK).
     */
    private static void startDeadlockScenario() {
        Thread threadA = new Thread(() -> {
            synchronized (LOCK_A) {
                sleepSilently(200);
                synchronized (LOCK_B) {
                    System.out.println("Недосяжний код для deadlock-A");
                }
            }
        }, "deadlock-A");

        Thread threadB = new Thread(() -> {
            synchronized (LOCK_B) {
                sleepSilently(200);
                synchronized (LOCK_A) {
                    System.out.println("Недосяжний код для deadlock-B");
                }
            }
        }, "deadlock-B");

        threadA.setDaemon(true);
        threadB.setDaemon(true);
        threadA.start();
        threadB.start();
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

