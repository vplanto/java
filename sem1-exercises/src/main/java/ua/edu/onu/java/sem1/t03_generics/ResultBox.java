package ua.edu.onu.java.sem1.t03_generics;

import java.util.NoSuchElementException;

/**
 * T03 (частина 1): Узагальнений контейнер результату операції (успіх або повідомлення про помилку).
 */
public final class ResultBox<T> {

    private final T value;
    private final String error;

    private ResultBox(T value, String error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Створює успішний результат. Якщо value == null - кидає IllegalArgumentException.
     */
    public static <T> ResultBox<T> ok(T value) {
        throw new UnsupportedOperationException("TODO: реалізувати ok");
    }

    /**
     * Створює результат з помилкою. Якщо message == null або порожній - кидає IllegalArgumentException.
     */
    public static <T> ResultBox<T> fail(String message) {
        throw new UnsupportedOperationException("TODO: реалізувати fail");
    }

    public boolean isSuccess() {
        throw new UnsupportedOperationException("TODO: реалізувати isSuccess");
    }

    /**
     * Повертає значення або кидає NoSuchElementException, якщо це результат з помилкою.
     */
    public T getValue() {
        throw new UnsupportedOperationException("TODO: реалізувати getValue");
    }

    /**
     * Повертає текст помилки або null, якщо результат успішний.
     */
    public String getError() {
        throw new UnsupportedOperationException("TODO: реалізувати getError");
    }

    /**
     * Повертає значення, якщо результат успішний, інакше fallback.
     */
    public T orElse(T fallback) {
        throw new UnsupportedOperationException("TODO: реалізувати orElse");
    }
}
