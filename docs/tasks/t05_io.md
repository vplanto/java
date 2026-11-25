# Завдання T05: Рядки та файловий I/O (`java.nio.file`)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Практикум 4](../p04_collections.md)
- **Код і тести:** [t05_io](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t05_io)
- **Команда перевірки:** `mvn test -Dtest=LogAnalyzerTest`

## Мета

Опанувати розбір рядків (`String`, `trim`, `toUpperCase`, `indexOf`/`split`) та безпечне читання/запис текстових файлів через сучасний API `java.nio.file.Files`.

## Що потрібно реалізувати

У класі `LogAnalyzer`:

1. `countByLevel(Path logFile)` - читає файл логів, де кожен валідний рядок має формат `LEVEL: message`. Ігнорує порожні рядки та рядки без `:`, нормалізує `LEVEL` до верхнього регістру та повертає `Map<String, Integer>` із кількістю записів кожного рівня.
2. `writeSummary(Map<String, Integer> counts, Path outputFile)` - формує відсортовані за ключем рядки формату `LEVEL=COUNT` і записує їх у `outputFile` в кодуванні UTF-8.

## Критерії приймання

- Усі тести в `LogAnalyzerTest` (включно з `@TempDir`) проходять успішно.
- Використано `java.nio.file.Files`, а ресурси (якщо відкривається `Stream<String>` через `Files.lines`) закриваються через `try-with-resources`.

[До реєстру завдань](index.md)
