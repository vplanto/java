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

Розбиваємо вхідні дані на класи еквівалентності — групи, де поведінка системи однакова для всіх значень всередині групи.

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

> 💡 **Зверніть увагу:** Ця таблиця рішень якраз і усуває ту «сіру зону» з нульовою ціною (`price = 0`), про яку ми говорили на початку лекції. Тепер у тестувальника та розробника є спільна домовленість: будь-яка ціна, що не є додатною, вважається помилкою і має генерувати виключення.

Кожен рядок — один тест. Жодного «а що, якщо» не пропущено.

### 2.4 State Transition Testing

Для систем зі складним життєвим циклом об'єктів тестуємо переходи між станами.

**Приклад (E-commerce):**
```
NEW (Нове) → PAID (Оплачене) → SHIPPED (Відправлене) → DELIVERED (Доставлене)
      ↘           ↘ 
       CANCELLED (Скасоване) → REFUNDED (Повернення коштів)
```

- **Позитивні тести:** перевірка дозволених переходів (`NEW` -> `PAID`, `PAID` -> `CANCELLED` -> `REFUNDED`).
- **Негативні тести:** спроба обійти логіку. Наприклад, повернення коштів для замовлення, які ще не були сплачені.

Тести:
- ✅ NEW → PAID (успішна оплата)
- ✅ PAID → SHIPPED (відправка після оплати)
- ✅ SHIPPED → DELIVERED (успішна доставка)
- ✅ PAID → CANCELLED → REFUNDED (скасування оплаченого замовлення з поверненням коштів)
- ❌ NEW → SHIPPED (спроба відвантаження без оплати)
- ❌ NEW → REFUNDED (неможливість «повернути» кошти за замовлення у статусі NEW, бо воно ще не оплачене)
- ❌ DELIVERED → CANCELLED (замовлення вже отримано клієнтом, його не можна просто скасувати — тільки процедура повернення товару)

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

ім'я тесту — це документація, яка читається при падінні білда. Використовуйте один із загальноприйнятих стандартів:

1. **MethodName_StateUnderTest_ExpectedBehavior**
   - `calculateDiscount_VipUser_Returns15Percent`
   - `shipOrder_UnpaidOrder_ThrowsException`

2. **should_ExpectedBehavior_When_StateUnderTest**
   - `should_return15PercentDiscount_when_userIsVip`
   - `should_throwException_when_shippingUnpaidOrder`

```java
// ❌  Погано: не зрозуміло, що саме зламалось
@Test void testDiscount() {}
@Test void test1() {}

// ✅  Добре: читається як специфікація (Behaviour-style)
@Test void should_return_90_percent_price_when_quantity_is_10_or_more() {}
@Test void should_throw_when_quantity_is_negative() {}
```

Коли тест падає в CI, розробник читає назву методу і вже розуміє, в чому проблема, навіть не відкриваючи код самого тесту.

---

## 4. Unit vs Integration: коли що використовувати

### 4.1 Коли Unit-тест достатній

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

### 4.2 Коли потрібен Integration-тест

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

Testcontainers піднімає реальний Docker-контейнер PostgreSQL (або будь-яку іншу БД) на час виконання тесту. Це гарантує, що ваші SQL-запити поводитимуться саме так, як на production.

**Налаштування через анотації:**
```java
@Testcontainers // Активує механізм Testcontainers в JUnit
class OrderRepositoryIntegrationTest {

    @Container // Створює та запускає контейнер автоматично
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void should_save_order_to_real_postgres() {
        // Тести запускаються проти РЕАЛЬНОГО PostgreSQL у докері
        // Немає проблем із несумісністю H2
    }
}
```

Плюс: тест ідентичний до реального оточення. Мінус: запуск докера займає час (мінус кілька секунд на старт).

---

## 5. Що тестувати в першу чергу

Не весь код однаково важливий. Розставляємо пріоритети:

**5.1 Найвищий пріоритет:**
- Фінансові розрахунки (округлення, конвертація, знижки).
- Авторизаційна логіка (хто що може бачити).
- Граничні стани переходів (замовлення, платіж, стан документа).
- Код, який вже ламався на production.

**5.2 Середній пріоритет:**
- Маппінг даних між шарами.
- Валідація вхідних даних.
- HTTP-контролери (happy path і основні помилки).

**5.3 Низький пріоритет (або не тестуємо):**
- Конфігураційні класи.
- DTO-класи (геттери/сетери).
- Generated code.

---

## 6. Flaky Tests: найбільший ворог CI/CD

Flaky test — тест, який іноді проходить, іноді падає без змін у коді.

Типові причини:

**6.1 Залежність від часу:**
Тести падають випадковим чином через використання `LocalDateTime.now()` всередині бізнес-логіки. Наприклад, логіка знижок, що залежить від поточної дати, зламається рівно опівночі або в іншому часовому поясі CI-сервера.
*   **Рішення:** використовувати фіксований `Clock` об'єкт у тестах, щоб "заморозити" час.

```java
// ❌ Зламається при зміні року або timezone
assertThat(order.getCreatedYear()).isEqualTo(2024);

// ✅ Використовуємо стабільне порівняння або мокаємо Clock
assertThat(order.getCreatedAt())
    .isCloseTo(Instant.now(), within(5, SECONDS));
```

**6.2 Порядок тестів:**
Тест залежить від стану, залишеного попереднім тестом (наприклад, запис у БД). Кожен тест має бути незалежним і очищати за собою дані (`@BeforeEach` або `@Transactional` rollback).

**6.3 Race conditions (Стан гонитви):**
Виникають в асинхронних процесах, коли тест намагається перевірити результат до того, як фонова задача встигла його записати.
*   **Рішення:** використовувати інструменти очікування (наприклад, Awaitility).

```java
// ❌ Async виклик може не встигнути виконатись до verify()
service.sendEmailAsync(user);
verify(emailClient).send(any()); 

// ✅ Чекаємо на виконання умови з тайм-аутом
await().atMost(5, SECONDS)
       .untilAsserted(() -> verify(emailClient).send(any()));
```

Flaky тест гірший за відсутній: команда звикає ігнорувати "червоний білд" («та це знову нестабільний тест»), і в цей момент справжня критична помилка проходить непоміченою.

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

H2 не підтримує весь SQL-діалект PostgreSQL. Наслідок: тест на H2 зелений, а на production — помилка синтаксису (через JSON-типи, специфічні індекси тощо).

Альтернатива: Testcontainers — бібліотека, що піднімає реальний Docker-контейнер PostgreSQL на час тесту. Тест повністю ідентичний production-середовищу.

</details>

**Питання 4: Що таке патерн AAA і чому важливо розділяти ці етапи в коді тесту?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

AAA (Arrange-Act-Assert) — це стандарт структури тесту:
1. **Arrange**: підготовка даних та моків.
2. **Act**: виклик методу, що тестується.
3. **Assert**: перевірка результату.

Розділення етапів робить тест читабельним. Якщо вони змішані, важко зрозуміти, яка саме дія призвела до результату. Чітке Arrange-Act-Assert дозволяє швидко знайти помилку в підготовці або в самому бізнесі.

</details>

**Питання 5: У чому різниця між Equivalence Partitioning та Decision Table, і коли слід застосовувати останнє?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

**Equivalence Partitioning** фокусується на розділенні ОДНОГО вхідного параметра на групи (наприклад, вік < 18, вік >= 18).

**Decision Table (Таблиця рішень)** використовується, коли результат залежить від КОМБІНАЦІЇ кількох умов (напр. вік + статус VIP + сума замовлення). Слід застосовувати її, коли бізнес-правила складні і легко пропустити специфічну комбінацію вхідних даних при звичайному переборі класів еквівалентності.

</details>

---

**[⬅️ P07: API Practice](p07_api_practice.md)** | **[Лекція 9: Docker ➡️](09_docker.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
