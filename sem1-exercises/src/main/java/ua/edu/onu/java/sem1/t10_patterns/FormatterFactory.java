package ua.edu.onu.java.sem1.t10_patterns;

/**
 * T10: Фабрика та декоратори для TextFormatter.
 */
public final class FormatterFactory {

    private FormatterFactory() {
    }

    /**
     * Повертає базову стратегію форматування за назвою:
     * - "upper": приводить рядок до верхнього регістру
     * - "lower": приводить рядок до нижнього регістру
     * - "trim": обрізає пробіли по краях
     * Для невідомої назви або null кидає IllegalArgumentException.
     */
    public static TextFormatter create(String type) {
        throw new UnsupportedOperationException("TODO: реалізувати Factory для upper/lower/trim");
    }

    /**
     * Декоратор: спершу застосовує delegate.format(input), а потім додає prefix на початку.
     */
    public static TextFormatter withPrefix(TextFormatter delegate, String prefix) {
        throw new UnsupportedOperationException("TODO: реалізувати Decorator withPrefix");
    }
}
