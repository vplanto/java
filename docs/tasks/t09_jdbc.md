# Завдання T09: Командний Git (Pull Request) та JDBC + H2

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 9](../09_jdbc.md), [Практикум 9](../p09_git.md), [Практикум 10](../p10_jdbc.md)
- **Код і тести:** [t09_jdbc](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t09_jdbc)
- **Команда перевірки:** `mvn test -Dtest=JdbcRepositoryTest`

## Мета

Реалізувати JDBC-репозиторій для вбудованої бази даних H2 із захистом від SQL-ін'єкцій через `PreparedStatement` та відпрацювати здачу коду через окрему Git-гілку й Pull Request.

## Що потрібно реалізувати

1. Створіть нову гілку `feat/t09-jdbc` від `main`.
2. У класі `BookRepository`:
   - `initSchema()` - створює таблицю `books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), author VARCHAR(255))`;
   - `save(String title, String author)` - вставляє рядок через `PreparedStatement` із `Statement.RETURN_GENERATED_KEYS` і повертає згенерований `id`;
   - `findByAuthor(String author)` - повертає список `Book` через параметризований `PreparedStatement` (спроба передати `' OR '1'='1` має повертати порожній список).
3. Відкрийте Pull Request із `feat/t09-jdbc` у `main` у власному репозиторії та прикріпіть посилання на PR у Classroom.

## Критерії приймання

- Усі тести в `JdbcRepositoryTest` проходять, включно з тестом на SQL-ін'єкцію.
- Усі JDBC-ресурси (`PreparedStatement`, `ResultSet`) закриваються через `try-with-resources`.
- Здача оформлена через окрему гілку та Pull Request.

[До реєстру завдань](index.md)
