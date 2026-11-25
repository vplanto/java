# Завдання T08: Багатопотоковість - синхронізація та `ExecutorService`

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 6](../06_threads.md), [Практикум 7](../p07_race_condition.md), [Практикум 8](../p08_executors.md)
- **Код і тести:** [t08_concurrency](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t08_concurrency)
- **Команда перевірки:** `mvn test -Dtest=ConcurrencyTest`

## Мета

Усунути стан гонитви (race condition) у спільному стані та навчитися запускати паралельні обчислення через пул потоків `ExecutorService` із коректним збиранням результатів через `Future`.

## Що потрібно реалізувати

1. У класі `SafeCounter`:
   - синхронізуйте `increment()` та `get()` (через `synchronized` або `AtomicInteger`), щоб при одночасній роботі 8 потоків не губився жоден інкремент.
2. У класі `ParallelSumTask`:
   - реалізуйте `sumInParallel(List<int[]> chunks, int threads)` через `Executors.newFixedThreadPool(threads)` і `Callable<Long>` / `Future<Long>`;
   - забезпечте гарантоване закриття пулу потоків (`try-with-resources` у Java 21 або `finally { pool.shutdown(); }`).

## Критерії приймання

- Усі тести в `ConcurrencyTest` стабільно проходять при багаторазовому запуску.
- Після завершення `sumInParallel` у JVM не залишається незавершених потоків пулу.

[До реєстру завдань](index.md)
