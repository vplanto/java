# Sem 1 Exercises - Незалежний практичний трек (Java 21 + JUnit 5)

Стартовий Maven-проєкт для практичних завдань 1-го семестру курсу «Технології розробки та тестування програмних продуктів» (ОНУ).

## Вимоги
- JDK 21 LTS
- Apache Maven 3.8+
- IntelliJ IDEA

## Як працювати з завданнями
1. Відкрийте директорію `sem1-exercises` як Maven-проєкт.
2. Перевірте середовище:
   ```bash
   mvn test -Dtest=EnvironmentSanityTest
   ```
3. Для кожного завдання `t01`-`t10` реалізуйте методи з `TODO` у `src/main/java/ua/edu/onu/java/sem1/<пакет>/` та запустіть відповідний тест:
   ```bash
   mvn test -Dtest=NumberStatsTest
   ```
4. Не змінюйте сигнатури публічних методів і не видаляйте перевірки у тестах.

## Відповідність пакетів завданням
- `t01_basics` - `NumberStatsTest` (T01)
- `t02_oop` - `DeviceContractTest` (T02)
- `t03_generics` - `GenericsTest` (T03)
- `t04_collections` - `CollectionsTest` (T04)
- `t05_io` - `LogAnalyzerTest` (T05)
- `t06_exceptions` - `ExceptionsTest` (T06)
- `t07_solid` - `SolidDesignTest` (T07)
- `t08_concurrency` - `ConcurrencyTest` (T08)
- `t09_jdbc` - `JdbcRepositoryTest` (T09)
- `t10_patterns` - `PatternsTest` (T10)
