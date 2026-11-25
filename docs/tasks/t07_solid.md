# Завдання T07: SOLID, DIP та модульний дизайн

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 5](../05_solid.md), [Практикум 5](../p05_solid.md), [Практикум 6](../p06_module_design.md)
- **Код і тести:** [t07_solid](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t07_solid)
- **Команда перевірки:** `mvn test -Dtest=SolidDesignTest`

## Мета

Застосувати принцип інверсії залежностей (DIP) та впровадження залежностей через конструктор (Constructor Injection), щоб бізнес-логіка тестувалася ізольовано за допомогою підставних реалізацій (fakes).

## Що потрібно реалізувати

У класі `OrderProcessor`:

1. Реалізуйте метод `checkout(String orderId, String customer, long subtotalCents)`:
   - перевірте, що `orderId` і `customer` не `null` і не порожні, а `subtotalCents >= 0` (інакше `IllegalArgumentException`);
   - обчисліть фінальну суму через `discountPolicy.apply(subtotalCents)`;
   - надішліть сповіщення через `notifier.send(customer, "Order " + orderId + " total: " + finalAmount)`;
   - поверніть `finalAmount`.

## Критерії приймання

- Усі тести в `SolidDesignTest` зелені.
- `OrderProcessor` не створює конкретні реалізації через `new` всередині себе та не пише напряму в `System.out`.

[До реєстру завдань](index.md)
