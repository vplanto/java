# Завдання T01: Базовий синтаксис, Maven та стиль коду

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 1](../01_what_is_java.md), [Лекція 1a](../01a_jvm_lifecycle.md), [Практикум 1](../p01_maven.md)
- **Код і тести:** [t01_basics](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t01_basics)
- **Команда перевірки:** `mvn test -Dtest=NumberStatsTest`

## Мета

Закріпити базовий синтаксис Java (примітиви, масиви, цикли, переповнення `int`), життєвий цикл збірки Maven та стандартний стиль коду (Java Code Conventions).

## Що потрібно реалізувати

У класі `ua.edu.onu.java.sem1.t01_basics.NumberStats` реалізуйте 4 статичні методи:

1. `sum(int[] values)` - повертає суму елементів у типі `long` без переповнення `int`; для порожнього масиву повертає `0L`, для `null` кидає `IllegalArgumentException`.
2. `max(int[] values)` - повертає максимальне значення непорожнього масиву (коректно працює з від'ємними числами); для `null` або порожнього масиву кидає `IllegalArgumentException`.
3. `countEven(int[] values)` - рахує кількість парних чисел (включно з `0` та від'ємними); для `null` кидає `IllegalArgumentException`.
4. `isPrime(int n)` - перевіряє, чи є число простим (`n >= 2`).

## Критерії приймання

- Усі тести в `NumberStatsTest` зелені.
- Дотримано Java Code Conventions (іменування змінних, відступи, без мертвого коду та `System.out.println`).
- Зміни зафіксовано окремим комітом `feat(t01): implement NumberStats`.

[До реєстру завдань](index.md)
