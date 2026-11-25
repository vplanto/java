package ua.edu.onu.java.sem1.t01_basics;

/**
 * T01: Базовий синтаксис, масиви, цикли та коректна робота з типами.
 */
public final class NumberStats {

    private NumberStats() {
    }

    /**
     * Повертає суму всіх елементів масиву без переповнення int.
     * Для порожнього масиву повертає 0L.
     * Якщо масив null - кидає IllegalArgumentException.
     */
    public static long sum(int[] values) {
        throw new UnsupportedOperationException("TODO: реалізувати sum");
    }

    /**
     * Повертає максимальне значення в непорожньому масиві.
     * Якщо масив null або порожній - кидає IllegalArgumentException.
     */
    public static int max(int[] values) {
        throw new UnsupportedOperationException("TODO: реалізувати max");
    }

    /**
     * Повертає кількість парних чисел у масиві.
     * Якщо масив null - кидає IllegalArgumentException.
     */
    public static int countEven(int[] values) {
        throw new UnsupportedOperationException("TODO: реалізувати countEven");
    }

    /**
     * Перевіряє, чи є число простим (n >= 2 і ділиться лише на 1 та саме себе).
     */
    public static boolean isPrime(int n) {
        throw new UnsupportedOperationException("TODO: реалізувати isPrime");
    }
}
