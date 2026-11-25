package ua.edu.onu.java.sem1.t04_collections;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * T04 (частина 2): Агрегація оцінок студентів через List, Set, Map.
 */
public class GradeBook {

    public record RecordEntry(String student, int score) {
    }

    /**
     * Додає запис про оцінку. Оцінка має бути в межах [0, 100], ім'я не порожнє.
     * Інакше кидає IllegalArgumentException.
     */
    public void addGrade(String student, int score) {
        throw new UnsupportedOperationException("TODO: реалізувати addGrade");
    }

    /**
     * Повертає незмінну історію всіх оцінок у порядку додавання.
     */
    public List<RecordEntry> getHistory() {
        throw new UnsupportedOperationException("TODO: реалізувати getHistory");
    }

    /**
     * Повертає множину унікальних імен студентів.
     */
    public Set<String> getUniqueStudents() {
        throw new UnsupportedOperationException("TODO: реалізувати getUniqueStudents");
    }

    /**
     * Повертає середній бал для кожного студента.
     */
    public Map<String, Double> getAverageByStudent() {
        throw new UnsupportedOperationException("TODO: реалізувати getAverageByStudent");
    }
}
