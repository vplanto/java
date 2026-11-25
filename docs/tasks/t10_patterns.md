# Завдання T10: Патерни проєктування (Factory, Strategy, Decorator)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 10](../10_design_patterns.md), [Лекція 7](../07_troubleshooting.md), [Лекція 8](../08_logging.md)
- **Код і тести:** [t10_patterns](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t10_patterns)
- **Команда перевірки:** `mvn test -Dtest=PatternsTest`

## Мета

Побудувати розширюваний конвеєр обробки тексту на основі трьох патернів GoF: Strategy (`TextFormatter`), Simple Factory (`FormatterFactory.create`) та Decorator (`FormatterFactory.withPrefix`).

## Що потрібно реалізувати

У класі `FormatterFactory`:

1. `create(String type)` - повертає реалізацію `TextFormatter` для значень `"upper"`, `"lower"`, `"trim"`; для `null` або невідомого типу кидає `IllegalArgumentException`.
2. `withPrefix(TextFormatter delegate, String prefix)` - повертає обгортку (`Decorator`), яка викликає `delegate.format(input)` і додає `prefix` на початку результату.

## Критерії приймання

- Усі тести в `PatternsTest` зелені.
- Композиція декораторів працює без зміни базових реалізацій (принцип OCP).

[До реєстру завдань](index.md)
