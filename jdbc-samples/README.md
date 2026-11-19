# JDBC Samples: Історія переходу "Банки" до простої реляційної БД

Демонстраційний Maven-проєкт до [Лекції 9: JDBC](../docs/09_jdbc.md).

## Історія

У попередніх практикумах (`BankJar`, `thread-samples`) ми зберігали донати лише в оперативній пам'яті (`InMemoryDonationRepository`). Після завершення програми дані зникають.

Цей модуль показує перехід до збереження стану в простій вбудованій БД **H2** (працює з однієї JAR-залежності без встановлення окремого сервера PostgreSQL/MySQL):

1. **DIP (інверсія залежностей):** `JarService` працює лише з інтерфейсом `DonationRepository` і не знає про SQL чи H2.
2. **Життєвий цикл JDBC та `try-with-resources`:** у `JdbcDonationRepository` реалізовано 5 кроків (`Connection` -> `PreparedStatement` -> `executeUpdate`/`executeQuery` -> `ResultSet` -> автоматичне закриття ресурсів).
3. **Безпека (`Statement` vs `PreparedStatement`):** метод `findByDonorUnsafe` демонструє SQL-ін'єкцію через конкатенацію рядка, а `findByDonor` захищає запит параметром `?`.
4. **Персистентність на диску:** `Main` підключається до файлової БД `jdbc:h2:file:./tmp/jar_db`, тому повторний запуск зберігає попередні донати; тести використовують ізольовану in-memory БД `jdbc:h2:mem:...`.

## Запуск

```bash
mvn test
```

Запуск консольної демонстрації:

```bash
mvn compile exec:java
```
