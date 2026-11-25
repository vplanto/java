package ua.edu.onu.java.sem1.t08_concurrency;

/**
 * T08 (частина 1): Потокобезпечний лічильник (усунення race condition).
 */
public class SafeCounter {

    private int value;

    /**
     * Атомарно збільшує лічильник на 1.
     */
    public void increment() {
        throw new UnsupportedOperationException("TODO: синхронізувати increment");
    }

    /**
     * Повертає поточне значення лічильника.
     */
    public int get() {
        throw new UnsupportedOperationException("TODO: повернути значення потокобезпечно");
    }
}
