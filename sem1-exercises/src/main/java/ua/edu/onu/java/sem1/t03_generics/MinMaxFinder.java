package ua.edu.onu.java.sem1.t03_generics;

import java.util.List;

/**
 * T03 (частина 2): Узагальнені методи з bounded type parameters.
 */
public final class MinMaxFinder {

    private MinMaxFinder() {
    }

    /**
     * Повертає максимальний елемент у непорожньому списку.
     * Якщо список null або порожній - кидає IllegalArgumentException.
     */
    public static <T extends Comparable<? super T>> T findMax(List<T> items) {
        throw new UnsupportedOperationException("TODO: реалізувати findMax");
    }

    /**
     * Повертає кількість елементів у списку, які строго більші за threshold.
     * Якщо items або threshold == null - кидає IllegalArgumentException.
     */
    public static <T extends Comparable<? super T>> int countGreaterThan(List<T> items, T threshold) {
        throw new UnsupportedOperationException("TODO: реалізувати countGreaterThan");
    }
}
