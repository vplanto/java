# Завдання T02: Класи, інтерфейси, наслідування та контракт `Object`

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 1b](../01b_jvm_memory_gc.md), [Практикум 2](../p02_class.md)
- **Код і тести:** [t02_oop](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t02_oop)
- **Команда перевірки:** `mvn test -Dtest=DeviceContractTest`

## Мета

Навчитися будувати ієрархію класів та інтерфейсів і коректно реалізовувати контракт `equals`, `hashCode` та `Comparable`, без якого об'єкти некоректно працюють у `HashSet`, `HashMap` і сортуванні.

## Що потрібно реалізувати

1. У базовому класі `Device`:
   - `compareTo(Device other)` - лексикографічне порівняння за `serialNumber`.
   - `equals(Object o)` та `hashCode()` - два пристрої рівні тоді й лише тоді, коли збігається їхній `serialNumber` (використовуйте `Objects.equals` / `Objects.hash`).
2. У класі `TemperatureSensor extends Device`:
   - `isHealthy()` - повертає `true`, якщо `lastReading` належить відрізку `[minTemp, maxTemp]`.
   - `statusSummary()` - повертає `"<serialNumber>: OK"` або `"<serialNumber>: ALERT"`.

## Критерії приймання

- Усі тести в `DeviceContractTest` проходять, включно з перевіркою `HashSet<Device>` та `Collections.sort`.
- Приватні поля інкапсульовані, валідація у конструкторах збережена.

[До реєстру завдань](index.md)
