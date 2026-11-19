# Практикум 10: JDBC на практиці та вбудована БД H2

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](DISCLAIMER.md).

## Експрес-опитування: Згадуємо основи

У [Практикумі 4 (Банка Monobank)](p04_collections.md) та [Практикумі 7](p07_race_condition.md) дані жили в оперативній пам'яті, а в [Лекції 9](09_jdbc.md) ми розібрали теорію JDBC.

1. Що станеться з балансом і списком донатів у програмі `BankJar` після перезапуску JVM?
2. Чим відрізняється `executeQuery()` від `executeUpdate()` у JDBC?
3. Чому у файлі `JdbcDonationRepository` не варто використовувати `Statement` із конкатенацією рядків для пошуку за ім'ям донатора?
4. Чому в інтерфейсі репозиторію небажано прокидати `throws SQLException` у бізнес-логіку?

<details markdown="1">
<summary>Наші очікування (відповідь)</summary>

1. Усі об'єкти з кучі (Heap) зникнуть, і при наступному старті "Банка" буде порожньою. Щоб стан зберігався між запусками, потрібне зовнішнє сховище (наприклад, реляційна БД).
2. `executeQuery()` виконує `SELECT` і повертає `ResultSet` (курсор таблиці результатів). `executeUpdate()` виконує `INSERT`, `UPDATE`, `DELETE` або DDL (`CREATE TABLE`) і повертає `int` (кількість змінених рядків).
3. Конкатенація введення користувача в SQL-рядок робить програму вразливою до **SQL-ін'єкції**. Потрібен `PreparedStatement` із параметрами `?`.
4. `SQLException` прив'язує високорівневий код (`JarService`) до конкретної технології збереження (JDBC), що порушує **DIP**. Краще обробляти `SQLException` у шарі репозиторію і загортати у власний доменний виняток (наприклад, `StorageException`).

</details>

-----

## Вступ та історія задачі

У попередніх практикумах (`BankJar`, `thread-samples`) ми зберігали донати у звичайному `List<Donation>` у пам'яті. Доки процес JVM працює - історія є. Щойно програма завершується або падає - усі записи втрачаються.

Бізнес-вимога: **донати мають зберігатися у реляційній базі даних і переживати перезапуск програми**, але без розгортання окремого сервера PostgreSQL на кожному студентському ноутбуці.

**Наша мета сьогодні:** пройти реальний шлях переходу від `InMemoryDonationRepository` до `JdbcDonationRepository` поверх вбудованої бази даних **H2**, розібрати її режими роботи, 5 кроків JDBC із `try-with-resources` і наочно відтворити SQL-ін'єкцію в тестах.

> Увесь робочий код цього практикуму знаходиться у Maven-модулі `jdbc-samples/`.
> Запуск тестів (JUnit 5):
> ```bash
> mvn -f jdbc-samples/pom.xml test
> ```
> Запуск консольної демонстрації (персистентність між запусками):
> ```bash
> mvn -f jdbc-samples/pom.xml compile exec:java
> ```

-----

## Що таке H2 Database і чому її використовують у Java

**H2** (`com.h2database:h2`) - це легка, швидка реляційна СУБД, написана повністю на Java. Для розробки та тестування на Java це один із найпоширеніших інструментів:

* **Один JAR без встановлення сервера:** не потрібно встановлювати PostgreSQL або піднімати Docker-контейнер. Достатньо додати залежність `h2` у `pom.xml`.
* **Драйвер Type-4 з коробки:** H2 реалізує JDBC-драйвер на 100% Java (`org.h2.Driver`) і автоматично реєструється через стандартний механізм Java SPI (`META-INF/services/java.sql.Driver`), тому виклик `Class.forName(...)` не потрібен.
* **Три режими роботи (керуються лише JDBC URL):**
  1. **Embedded In-Memory (`jdbc:h2:mem:jar_db`):** база живе виключно в оперативній пам'яті JVM. Після зупинки програми дані зникають, але тести працюють за мілісекунди і не залишають сміття на диску.
  2. **Embedded File (`jdbc:h2:file:./tmp/jar_db`):** база працює всередині вашого процесу JVM, але зберігає таблиці та індекси у локальному файлі (`jar_db.mv.db`). При повторному запуску `Main` дані залишаються на місці.
  3. **Server / TCP (`jdbc:h2:tcp://localhost/~/test`):** H2 може працювати і як окремий сервер для кількох клієнтів одночасно.
* **Режим сумісності (Compatibility Mode):** параметром у URL (наприклад, `jdbc:h2:mem:test;MODE=PostgreSQL`) H2 імітує діалект PostgreSQL або MySQL, що робить її стандартом для швидких інтеграційних тестів у Spring Boot (2-й семестр).

### Ключова пастка In-Memory H2: `DB_CLOSE_DELAY=-1`

За замовчуванням H2 у режимі `mem:` знищує базу даних (разом з усіма таблицями і рядками) у ту мить, **коли закривається останнє активне з'єднання `Connection`**.

Якщо ваш репозиторій на кожний запит відкриває і закриває власний `Connection` через `try-with-resources`:
* у `initSchema()` з'єднання відкрилось, створило таблицю `donations` і закрилось;
* якщо URL просто `jdbc:h2:mem:jar_db`, таблиця миттєво зникає, і наступний `save()` впаде з помилкою `Table "DONATIONS" not found`.

Додавання параметра **`DB_CLOSE_DELAY=-1`** в URL (`jdbc:h2:mem:jar_db;DB_CLOSE_DELAY=-1`) змушує H2 тримати базу в пам'яті до завершення роботи JVM, навіть коли між запитами немає жодного відкритого `Connection`. Для тестів ми також додаємо унікальне ім'я (`UUID.randomUUID()`), щоб кожен тест отримував чисту ізольовану базу.

-----

## Архітектура проєкту `jdbc-samples` (DIP у дії)

Модуль побудований так, щоб бізнес-логіка не залежала від SQL, драйвера чи конкретної БД:

```text
jdbc-samples/
├── pom.xml                                  // H2 2.3.232 + JUnit 5 + surefire + exec
└── src/
    ├── main/java/org/example/jdbc/
    │   ├── Main.java                        // Збирач залежностей і демонстрація
    │   ├── model/Donation.java              // record (id, donorName, amountKopiyky, createdAt)
    │   ├── repository/
    │   │   ├── DonationRepository.java      // Інтерфейс (DIP)
    │   │   ├── StorageException.java        // RuntimeException-обгортка над SQLException
    │   │   ├── InMemoryDonationRepository.java // Варіант у пам'яті
    │   │   └── JdbcDonationRepository.java  // Реалізація на чистому JDBC
    │   └── service/JarService.java          // Бізнес-валідація, залежить лише від інтерфейсу
    └── test/java/org/example/jdbc/
        └── JdbcDonationRepositoryTest.java  // 4 JUnit-тести (CRUD, SQL-ін'єкція, DIP)
```

Ключовий момент проєктування: `JarService` приймає у конструкторі інтерфейс `DonationRepository`. Він однаково працює і з `InMemoryDonationRepository`, і з `JdbcDonationRepository`, і з майбутнім `PostgresDonationRepository`.

-----

## Як працює `JdbcDonationRepository` крок за кроком

### 1. Підключення та створення схеми

При створенні екземпляра `JdbcDonationRepository` виконується `initSchema()`:

```java
private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, user, password);
}

private void initSchema() {
    try (Connection conn = openConnection();
         Statement stmt = conn.createStatement()) {
        stmt.execute(CREATE_TABLE_SQL);
    } catch (SQLException e) {
        throw new StorageException("Не вдалося ініціалізувати таблицю donations", e);
    }
}
```

* `DriverManager.getConnection(...)` за префіксом `jdbc:h2:...` знаходить завантажений драйвер H2 і відкриває з'єднання.
* DDL-запит `CREATE TABLE IF NOT EXISTS donations (...)` створює таблицю з автоінкрементним ключем `id BIGINT AUTO_INCREMENT PRIMARY KEY`.
* Зверніть увагу на `try (...)`: і `Statement`, і `Connection` реалізують інтерфейс `AutoCloseable`, тому JVM гарантовано закриє їх у зворотному порядку (`stmt.close()`, потім `conn.close()`) навіть у разі винятку.

### 2. Запис даних через `PreparedStatement`

```java
String sql = "INSERT INTO donations (donor_name, amount_kopiyky, created_at) VALUES (?, ?, ?)";
try (Connection conn = openConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, donorName);
    ps.setLong(2, amountKopiyky);
    ps.setTimestamp(3, Timestamp.from(Instant.now()));
    ps.executeUpdate();
}
```

* Знаки питання `?` - це плейсхолдери (індексація параметрів у JDBC починається з **1**, а не з 0).
* Типізовані методи `setString`, `setLong`, `setTimestamp` передають значення окремо від структури SQL-запиту.
* Для `INSERT`/`UPDATE`/`DELETE` викликається `executeUpdate()`.

### 3. Читання даних і мапінг `ResultSet`

```java
String sql = "SELECT id, donor_name, amount_kopiyky, created_at FROM donations WHERE donor_name = ? ORDER BY id";
try (Connection conn = openConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, donorName);
    try (ResultSet rs = ps.executeQuery()) {
        return mapRows(rs);
    }
}
```

* Спочатку `ResultSet` стоїть **перед** першим рядком. Перший виклик `rs.next()` пересуває курсор на перший рядок і повертає `true`, якщо дані є.
* У методі `mapRows(rs)` кожен рядок таблиці перетворюється на Java-об'єкт `Donation(...)`.
* Вкладений `try (ResultSet rs = ps.executeQuery())` гарантує закриття курсора `ResultSet` до закриття `PreparedStatement` і `Connection` (важливо, коли параметри `?` встановлюються до виклику `executeQuery()`).

### 4. Жива демонстрація SQL-ін'єкції (`Statement` vs `PreparedStatement`)

У класі `JdbcDonationRepository` навмисно залишено антиприклад `findByDonorUnsafe(String donorName)`:

```java
String sql = "SELECT id, donor_name, amount_kopiyky, created_at FROM donations WHERE donor_name = '"
        + donorName + "' ORDER BY id";
```

Якщо користувач передає рядок `' OR '1'='1`, сформований SQL перетворюється на:

```sql
SELECT id, donor_name, amount_kopiyky, created_at
FROM donations
WHERE donor_name = '' OR '1'='1'
ORDER BY id
```

Оскільки `'1'='1'` завжди істинно, `findByDonorUnsafe` повертає **усі** записи таблиці замість порожнього списку. Натомість `findByDonor` через `PreparedStatement` шукає донатора з буквальним ім'ям `' OR '1'='1` і безпечно повертає 0 записів.

**Важливе обмеження плейсхолдерів `?`:** параметри `PreparedStatement` працюють **лише для значень** (`WHERE col = ?`, `VALUES (?)`), але **не** для назв таблиць або колонок. Якщо ви формуєте запит динамічно за метаданими (`DatabaseMetaData`), імена таблиць і колонок заборонено конкатенувати без перевірки за білим списком (allow-list) допустимих ідентифікаторів.

-----

## Персистентність між запусками (`Main` vs Тести)

1. Запустіть `mvn -f jdbc-samples/pom.xml compile exec:java` уперше - `Main` побачить порожню таблицю, додасть два донати (`Олена`, `Андрій`) і збереже файл `./tmp/jar_db.mv.db`.
2. Запустіть команду вдруге - `Main` прочитає попередні два записи з диска, додасть третій (`Повторний запуск`) і покаже оновлену суму (`75000 коп.`).
3. У тестах (`JdbcDonationRepositoryTest`) використовується `jdbc:h2:mem:jar_<UUID>;DB_CLOSE_DELAY=-1`: кожен тестовий метод отримує чисту БД в пам'яті і не впливає на сусідні тести.

-----

## Контрольні питання

1. Чим відрізняються режими H2 `jdbc:h2:mem:...` та `jdbc:h2:file:...`, і навіщо в in-memory режимі додають `DB_CLOSE_DELAY=-1`?
2. Чому в `JdbcDonationRepository` параметр `ps.setString(1, ...)` встановлюється до відкриття `try (ResultSet rs = ps.executeQuery())`?
3. Чому `PreparedStatement` повністю захищає від SQL-ін'єкції у `WHERE donor_name = ?`, але не вирішує проблему, якщо ім'я колонки в `ORDER BY` приходить від користувача?
4. Як `StorageException` допомагає дотриматися принципу інверсії залежностей (DIP) у `JarService`?

<details markdown="1">
<summary>Відповіді (спробуйте спочатку відповісти самі)</summary>

1. `mem:` тримає БД у RAM (зникає після зупинки JVM, ідеально для тестів), `file:` зберігає дані на диску у `.mv.db` (переживає перезапуск). Без `DB_CLOSE_DELAY=-1` H2 очищує in-memory БД одразу після закриття останнього `Connection` (тобто між `initSchema()` і `save()` таблиця зникне).
2. Тому що запит із параметрами `?` не можна виконати до підстановки значень. Вкладений `try-with-resources` спочатку дозволяє налаштувати `PreparedStatement`, а потім гарантовано закриває `ResultSet` перед закриттям самого `PreparedStatement` і `Connection`.
3. Драйвер і СУБД трактують `?` виключно як скалярне значення, а не частину SQL-граматики. Імена таблиць і колонок є ідентифікаторами структури запиту, тому плейсхолдери для них не працюють - потрібна перевірка за фіксованим білим списком (allow-list) на стороні Java.
4. Якби інтерфейс `DonationRepository` оголошував `throws SQLException`, бізнес-сервіс `JarService` був би змушений імпортувати `java.sql.SQLException` і знати, що під капотом саме JDBC. Обгортка `StorageException` приховує деталі реалізації за абстракцією.

</details>

-----

**[Лекція 9: JDBC](09_jdbc.md)** | **[Головне меню курсу](index.md)**
