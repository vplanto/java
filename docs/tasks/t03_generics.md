# Завдання T03: Узагальнення (Generics) та обмеження типів

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Практикум 3](../p03_generics.md)
- **Код і тести:** [t03_generics](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t03_generics)
- **Команда перевірки:** `mvn test -Dtest=GenericsTest`

## Мета

Створити типобезпечний узагальнений контейнер та узагальнені алгоритми з обмеженням типів (`<T extends Comparable<? super T>>`) без використання сирих типів (raw types) і небезпечних приведеннь.

## Що потрібно реалізувати

1. У класі `ResultBox<T>`:
   - фабричні методи `ok(T value)` та `fail(String message)` із перевіркою аргументів (`IllegalArgumentException`);
   - `isSuccess()`, `getValue()` (кидає `NoSuchElementException` для помилкового результату), `getError()`, `orElse(T fallback)`.
2. У класі `MinMaxFinder`:
   - `findMax(List<T> items)` - знаходить максимум у непорожньому списку;
   - `countGreaterThan(List<T> items, T threshold)` - повертає кількість елементів, строго більших за `threshold`.

## Критерії приймання

- Усі тести в `GenericsTest` проходять без попереджень компілятора (`unchecked` / raw types).
- Код працює для довільних `Comparable`-типів (`Integer`, `String`, власні класи).

[До реєстру завдань](index.md)
