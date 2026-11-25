# Завдання T06: Обробка винятків та захист інваріантів

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 4](../04_java_exceptions.md), [приклади exceptions-samples](https://github.com/vplanto/java/tree/main/exceptions-samples)
- **Код і тести:** [t06_exceptions](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t06_exceptions)
- **Команда перевірки:** `mvn test -Dtest=ExceptionsTest`

## Мета

Навчитися розрізняти unchecked-помилки контракту (`IllegalArgumentException`) та checked-бізнес-винятки (`InsufficientFundsException`), зберігаючи цілісність стану об'єкта при аварійному завершенні операції.

## Що потрібно реалізувати

У класі `WalletAccount` (баланс у копійках, тип `long`):

1. `deposit(long amountCents)` - додає кошти; при `amountCents <= 0` кидає `IllegalArgumentException`.
2. `withdraw(long amountCents)` - при `amountCents <= 0` кидає `IllegalArgumentException`; при нестачі коштів кидає `InsufficientFundsException(amountCents, balanceCents)` без зміни балансу.
3. `transferTo(WalletAccount target, long amountCents)` - перевіряє `target != null` та `target != this` (`IllegalArgumentException`), виконує зняття з поточного рахунку та зарахування на `target`. У разі помилки обидва баланси лишаються незмінними.

## Критерії приймання

- Усі тести в `ExceptionsTest` проходять.
- Немає порожніх блоків `catch` або перехоплення `Exception` без повторного прокидання.

[До реєстру завдань](index.md)
