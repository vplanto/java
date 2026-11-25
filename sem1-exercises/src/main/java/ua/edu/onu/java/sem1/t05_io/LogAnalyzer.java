package ua.edu.onu.java.sem1.t05_io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * T05: Рядки та файловий I/O (java.nio.file).
 * Формат рядка логу: "LEVEL: message", наприклад "ERROR: disk full".
 */
public final class LogAnalyzer {

    private LogAnalyzer() {
    }

    /**
     * Рахує кількість записів кожного рівня логування (ключ у верхньому регістрі: INFO, WARN, ERROR).
     * Порожні рядки та рядки без ':' ігноруються.
     */
    public static Map<String, Integer> countByLevel(Path logFile) throws IOException {
        throw new UnsupportedOperationException("TODO: реалізувати countByLevel через Files.readAllLines / lines");
    }

    /**
     * Записує зведення у файл у відсортованому за ключем порядку: кожен рядок формату "LEVEL=COUNT".
     */
    public static void writeSummary(Map<String, Integer> counts, Path outputFile) throws IOException {
        throw new UnsupportedOperationException("TODO: реалізувати writeSummary через Files.writeString / write");
    }
}
