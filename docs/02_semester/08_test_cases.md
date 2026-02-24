# Лекція 8: Test Cases & Coverage. Як писати тести, що дійсно ловлять баги

**Аудиторія:** 2-й курс (Junior Strong)
**Ціль:** Навчитись писати тест-кейси так, щоб вони знаходили реальні проблеми — а не просто підтверджували, що «happy path» працює.

> **English version:** [English](en/08_test_cases.md)

---

## 1. Відкрита дискусія (Warm-up)

Функція розраховує знижку:

```java
public BigDecimal calculateDiscount(BigDecimal price, int quantity) {
    if (quantity >= 10) return price.multiply(BigDecimal.valueOf(0.9));
    return price;
}
```

Розробник написав один тест:

```java
@Test
void should_apply_discount_for_10_items() {
    BigDecimal result = service.calculateDiscount(new BigDecimal("100"), 10);
    assertThat(result).isEqualByComparingTo("90");
}
```

Питання до групи: скільки сценаріїв цей тест не перевіряє? Які з них можуть зламати систему на production?

<details markdown="1">
<summary>Розгорнути відповідь</summary>

Непокриті сценарії:

- `quantity = 0` — що повертається?
- `quantity = -1` — від'ємна кількість. Чи є перевірка?
- `quantity = 9` — граничне значення. Знижки немає?
- `quantity = 11` — знижка є?
- `price = null` — NullPointerException?
- `price = 0` — знижка від нуля = нуль, це ок?
- `price = -100` — від'ємна ціна?
- `quantity = Integer.MAX_VALUE` — overflow?

«Happy path» тест дає 100% coverage цієї функції. І не ловить жодного з цих сценаріїв.

</details>

---

## 2. Техніки проєктування тест-кейсів

### 2.1 Equivalence Partitioning

Розбиваємо вхідні дані на класи еквівалентності — grupи, де поведінка системи однакова для всіх значень всередині групи.

Для функції `calculateDiscount(price, quantity)`:

```
Клас 1: quantity < 0     → очікуємо виключення або 0
Клас 2: quantity = 0     → очікуємо виключення або повну ціну
Клас 3: 1 ≤ quantity < 10 → повна ціна (без знижки)
Клас 4: quantity ≥ 10    → ціна з 10% знижкою
```

Достатньо одного тесту на клас. Тестувати `quantity=3`, `quantity=5`, `quantity=7` — зайве, вони в одному класі.

### 2.2 Boundary Value Analysis

Баги живуть на кордонах. Перевіряємо значення на межі між класами:

```
Для quantity:
  -1  → граничне в «некоректному» класі
   0  → граничне в «нульовому» класі
   1  → перший елемент «нормального» класу
   9  → останній без знижки
  10  → перший зі знижкою ← найбільш імовірний баг тут!
  11  → другий зі знижкою
```

Більшість off-by-one помилок (`>=` замість `>`) ловляться граничними значеннями.

### 2.3 Decision Table

Якщо є кілька умов, будуємо таблицю рішень:

| price | quantity | isVip | Знижка | Результат |
| :--- | :--- | :--- | :--- | :--- |
| > 0 | >= 10 | true | 15% | price × 0.85 |
| > 0 | >= 10 | false | 10% | price × 0.90 |
| > 0 | < 10 | true | 5% | price × 0.95 |
| > 0 | < 10 | false | 0% | price |
| <= 0 | будь-яке | будь-яке | - | Виключення |

Кожен рядок — один тест. Жодного «а що, якщо» не пропущено.

### 2.4 State Transition Testing

Для систем зі станом тестуємо переходи:

```
Замовлення:
DRAFT → CONFIRMED → PAID → SHIPPED → DELIVERED
                ↘
                 CANCELLED

Тести:
✅ DRAFT → CONFIRMED (нормальний шлях)
✅ CONFIRMED → CANCELLED (скасування після підтвердження)
✅ PAID → CANCELLED (скасування після оплати — має повернути гроші)
❌ DELIVERED → CONFIRMED (неможливий перехід — система має відмовити)
❌ SHIPPED → DRAFT (неможливий — перевіряємо, що не можна «відмотати»)
```

---

## 3. Структура хорошого тесту: AAA

Кожен тест має чітку структуру:

```java
@Test
void should_throw_exception_when_quantity_is_negative() {
    // Arrange — підготовка
    PricingService service = new PricingService();
    BigDecimal price = new BigDecimal("100");
    int invalidQuantity = -1;

    // Act — виконання
    ThrowableAssert.ThrowingCallable action =
        () -> service.calculateDiscount(price, invalidQuantity);

    // Assert — перевірка
    assertThatThrownBy(action)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Quantity must be positive");
}
```

Arrange-Act-Assert (або Given-When-Then) — не просто формальність. Тест, де ці три частини нерозрізнені, важко читати і важко підтримувати.

### Іменування тестів

Ім'я тесту — це документація, яка читається при падінні білда:

```java
// ❌  Погано: не зрозуміло, що перевіряється
@Test void testDiscount() {}
@Test void test1() {}

// ✅  Добре: читається як специфікація
@Test void should_return_90_percent_price_when_quantity_is_10_or_more() {}
@Test void should_throw_when_quantity_is_negative() {}
@Test void should_return_full_price_when_quantity_is_9() {}
```

Коли тест падає в CI, розробник читає ім'я і вже розуміє, що зламалось — без відкриття коду.

---

## 4. Unit vs Integration: коли що використовувати

### Коли Unit-тест достатній

- Бізнес-логіка без зовнішніх залежностей.
- Математичні розрахунки (знижки, конвертація).
- Валідація формату (email, телефон).
- Трансформація даних (маппінг об'єктів).

```java
// ✅ Чистий unit-тест: немає БД, немає HTTP
@Test
void should_map_user_entity_to_dto() {
    UserEntity entity = new UserEntity(1L, "alice@mail.com", UserType.PREMIUM);
    UserDto dto = mapper.toDto(entity);
    assertThat(dto.getEmail()).isEqualTo("alice@mail.com");
    assertThat(dto.getIsPremium()).isTrue();
}
```

### Коли потрібен Integration-тест

- Взаємодія з реальною БД (SQL-запити, транзакції).
- HTTP-шар (контролер, серіалізація/десеріалізація).
- Kafka/RabbitMQ consumer/producer.
- Зовнішні API (через WireMock або Testcontainers).

```java
@DataJpaTest  // Підіймає тільки JPA-шар, без веб
class UserRepositoryTest {

    @Autowired UserRepository repository;

    @Test
    void should_find_premium_users_by_status() {
        // Дані вставляються в тестову БД (H2 або Testcontainers)
        repository.save(new UserEntity("alice@mail.com", UserType.PREMIUM));
        repository.save(new UserEntity("bob@mail.com", UserType.STANDARD));

        List<UserEntity> premiums = repository.findByType(UserType.PREMIUM);

        assertThat(premiums).hasSize(1);
        assertThat(premiums.get(0).getEmail()).isEqualTo("alice@mail.com");
    }
}
```

### Testcontainers: реальна БД у тестах

Testcontainers піднімає реальний Docker-контейнер PostgreSQL (або будь-що інше) під час тесту:

```java
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15");

    // Тести запускаються проти реального PostgreSQL
    // Ніяких H2-несумісностей
}
```

Плюс: тест ідентичний до production. Мінус: повільніше, ніж H2. Рішення: запускати в CI, а не на кожен локальний запуск.

---

## 5. Що тестувати в першу чергу

Не весь код однаково важливий. Розставляємо пріоритети:

**Найвищий пріоритет:**
- Фінансові розрахунки (округлення, конвертація, знижки).
- Авторизаційна логіка (хто що може бачити).
- Граничні стани переходів (замовлення, платіж, стан документа).
- Код, який вже ламався на production.

**Середній пріоритет:**
- Маппінг даних між шарами.
- Валідація вхідних даних.
- HTTP-контролери (happy path і основні помилки).

**Низький пріоритет (або не тестуємо):**
- Конфігураційні класи.
- DTO-класи (геттери/сетери).
- Generated code.

---

## 6. Flaky Tests: найбільший ворог CI/CD

Flaky test — тест, який іноді проходить, іноді падає без змін у коді.

Типові причини:

**Залежність від часу:**
```java
// ❌ Зламається в новому році або при зміні timezone
assertThat(order.getCreatedYear()).isEqualTo(2024);

// ✅ Порівнюємо з поточним часом
assertThat(order.getCreatedAt())
    .isCloseTo(Instant.now(), within(5, SECONDS));
```

**Порядок тестів:**
Тест залежить від стану, залишеного попереднім тестом. Кожен тест має бути незалежним і залишати систему в чистому стані (`@BeforeEach`, `@Transactional` rollback).

**Race conditions:**
```java
// ❌ Async виклик без очікування результату
service.sendEmailAsync(user);
verify(emailClient).send(any()); // Може ще не виконатись!

// ✅ Awaiting з timeout
await().atMost(5, SECONDS)
       .untilAsserted(() -> verify(emailClient).send(any()));
```

Flaky тест гірший за відсутній тест: команда починає ігнорувати червоний CI («та це знову flaky»), і тоді реальний баг проходить непоміченим.

---

## 7. Екзаменаційний пул (Exam Questions)

**Питання 1: Поясніть Boundary Value Analysis. Чому межі важливіші за середні значення?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Boundary Value Analysis (BVA) — техніка вибору тест-кейсів на межах між класами еквівалентності.

Більшість off-by-one помилок виникають саме на межах: розробник пише `>` замість `>=`, або `<= 10` замість `< 10`. Середні значення клас перевіряють однаково, межові — де саме починається і закінчується кожен клас.

Для умови `quantity >= 10`: тестуємо 9 (без знижки), 10 (зі знижкою), 11 (зі знижкою). Значення 5 або 20 не дають додаткової інформації.

</details>

**Питання 2: Що таке Flaky Test? Яка найнебезпечніша його властивість?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Flaky Test — тест, що іноді проходить, іноді падає без змін у коді. Причини: залежність від системного часу, порядку виконання, async-операцій, зовнішніх сервісів.

Найнебезпечніша властивість: команда починає ігнорувати червоний CI. Коли всі знають, що «деякі тести іноді падають», реальний баг проходить як «чергова нестабільність». Flaky tests руйнують довіру до CI/CD як засобу захисту.

</details>

**Питання 3: Коли H2 (in-memory БД) для тестів є проблемою? Яка альтернатива?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

H2 не підтримує весь SQL-діалект PostgreSQL. Специфічні конструкції (JSON-операції, window functions, PostgreSQL-specific types) можуть не працювати або поводитись інакше.

Наслідок: тест на H2 зелений, а на production (PostgreSQL) — помилка синтаксису.

Альтернатива: Testcontainers — бібліотека, що піднімає реальний Docker-контейнер PostgreSQL на час тесту. Тест повністю ідентичний production-середовищу.

</details>

---

**[⬅️ Лекція 7: API Design](07_api_design.md)** | **[Лекція 9: Docker ➡️](09_docker.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
