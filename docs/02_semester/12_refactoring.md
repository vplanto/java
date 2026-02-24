# Лекція 12: Refactoring. Код, який не соромно читати через рік

**Аудиторія:** 2-й курс (Junior Strong)
**Ціль:** Зрозуміти, що рефакторинг — це не переписування з нуля, а дисциплінована практика. Навчитись розпізнавати Code Smells, управляти Technical Debt і рефакторити безпечно під захистом тестів.

> **English version:** [English](en/12_refactoring.md) | **Незнайоме слово?** → [Глосарій курсу](glossary.md)

---

## 1. Відкрита дискусія (Warm-up)

Уявіть: ви повертаєтесь до коду, який написали 6 місяців тому. Або ще гірше — до коду, що написав хтось інший, хто вже пішов з компанії.

> Питання: як довго триватиме, поки ви зрозумієте, *що* цей код робить і *чому* він саме такий?

<details markdown="1">
<summary>Розгорнути відповідь</summary>

Якщо відповідь — «кілька годин» або «я не впевнений, що я взагалі можу це змінити без поламки» — це симптом. Код накопичив **Technical Debt**.

Гарний код — це не код без помилок. Це код, який легко **читати**, **розуміти** і **змінювати**. Рефакторинг — це систематичний процес покращення цих трьох властивостей.

</details>

---

## 2. Що таке рефакторинг (і що ним не є)

Martin Fowler, «Refactoring» (1999, 2018):

> **Іменник:** «Зміна внутрішньої структури програмного забезпечення, щоб зробити його легшим для розуміння і дешевшим для модифікації, без зміни зовнішньої поведінки.»

> **Дієслово:** «Реструктурувати ПЗ, застосовуючи серію рефакторингів, без зміни зовнішньої поведінки.»

### Що НЕ є рефакторингом

| Це | Чому не рефакторинг |
| :--- | :--- |
| Виправити баг | Змінює поведінку — баг зникає |
| Оптимізувати швидкість | Часто змінює спостережувану поведінку (latency) |
| Переписати модуль з нуля | Це нова розробка, не рефакторинг |
| Додати нову фічу | Розширює поведінку |

**Золоте правило:** до і після рефакторингу всі тести мають проходити — і жоден новий не додається.

---

## 3. Technical Debt: як виникає і чому дорожчає

**Технічний борг** — це ціна швидких компромісів. Обираєш «зробити зараз» замість «зробити правильно» — накопичуєш борг. Як кредит: тіло малесеньке, але відсотки ростуть.

### Джерела технічного боргу

| Причина | Приклад |
| :--- | :--- |
| Тиск дедлайну | «Зробимо потім» (потім не настає) |
| Відсутність тестів | Боїшся щось змінити — раптом впаде |
| Відсутність документації | Нова людина витрачає тиждень, щоб зрозуміти модуль |
| Довгі паралельні гілки | Merge conflict на 2000 рядків |
| Відсутність code review | Кожен пише по-своєму |
| Недосвідченість команди | «Головне, щоб тест проходив» |

### Як вимірювати борг

SonarQube — найпоширеніший інструмент аналізу якості коду. Він показує:
- **Code Smells** — підозрілі ділянки
- **Technical Debt** — оцінка в годинах/днях на виправлення
- **Coverage** — відсоток покриття тестами
- **Duplications** — дубльований код

---

## 4. Code Smells: діагностика проблем

«Code Smell» — це ознака, що щось може бути не так. Не гарантована помилка, але сигнал «тут варто подивитись уважніше».

### Категорії Code Smells (за Fowler)

**Роздуті (Bloaters)** — код, що виріс більше, ніж мав:
- `Long Method` — метод на 200 рядків. Правило: метод має бути читабельним без прокрутки.
- `Large Class` — клас, що відповідає за занадто багато (порушення SRP).
- `Long Parameter List` — функція з 8 параметрами. Передайте об'єкт.
- `Data Clumps` — одні й ті ж 3 поля скрізь: `city`, `street`, `zip`. Зробіть `Address`.

**Зловживання ООП (OO Abusers):**
- `Switch Statement` — довгий switch по типу. Замінюється поліморфізмом.
- `Temporary Field` — поле класу, яке заповнюється тільки в одному методі.

**Заважають змінам (Change Preventers):**
- `Divergent Change` — одна правка вимагає змін у 10 різних місцях класу.
- `Shotgun Surgery` — одна правка вимагає змін у 10 різних класах.

**Зайве (Dispensables):**
- `Comments` — коментар, що пояснює *що* робить код (замість *чому*). Хороший код читається без коментарів.
- `Dead Code` — закоментований або невикористаний код. Видаліть — він в Git.
- `Duplicate Code` — одна логіка скопійована в двох місцях. Будь-яка правка потребує синхронізації обох копій.

**Зв'язування (Couplers):**
- `Feature Envy` — метод класу A постійно викликає методи класу B. Можливо, цей метод має бути в B.
- `Inappropriate Intimacy` — два класи знають занадто багато про internal-стан одне одного.

---

## 5. Коли рефакторити (і коли ні)

### Коли рефакторити ✅

```
Перед додаванням нової фічі     → спочатку приберіть бардак, потім додавайте
Коли код важко зрозуміти        → якщо читаєш і думаєш «що це?» — рефакторти
Після того як всі тести пройшли → безпечна точка для змін
Під час Code Review              → не потужний рефакторинг, але дрібні покращення
```

### Коли НЕ рефакторити ❌

```
Код не працює              → спочатку зробіть його робочим, потім чистим
Тести падають              → не можна перевіряти результат рефакторингу
Жорсткий дедлайн завтра   → технічний борг — але прийнятний компроміс
Функціонал буде викинутий  → не витрачайте час на код, який незабаром видалять
Код і так хороший          → не рефакторте заради рефакторингу
```

> **Правило Boy Scout:** «Залишай код чистішим, ніж знайшов». Кожен раз, торкаючись файлу, покращуй його трохи. Невеликі кроки щодня > грандіозний рефакторинг раз на рік.

---

## 6. Базові техніки рефакторингу

### Extract Method (Виокремлення методу)

Найчастіша техніка. Занадто довгий метод розбивається на менші.

```java
// ❌ До: один метод робить все
public void printOrder(Order order) {
    System.out.println("=== Order #" + order.getId() + " ===");
    System.out.println("Customer: " + order.getCustomer().getName());
    System.out.println("Email: " + order.getCustomer().getEmail());
    double total = 0;
    for (Item item : order.getItems()) {
        total += item.getPrice() * item.getQuantity();
        System.out.println("  " + item.getName() + " x" + item.getQuantity());
    }
    if (order.hasDiscount()) total *= 0.9;
    System.out.println("Total: " + total);
}

// ✅ Після: кожен метод — одна відповідальність
public void printOrder(Order order) {
    printHeader(order);
    printCustomer(order.getCustomer());
    printItems(order.getItems());
    printTotal(calculateTotal(order));
}

private void printHeader(Order order) {
    System.out.println("=== Order #" + order.getId() + " ===");
}

private double calculateTotal(Order order) {
    double total = order.getItems().stream()
        .mapToDouble(i -> i.getPrice() * i.getQuantity())
        .sum();
    return order.hasDiscount() ? total * 0.9 : total;
}
```

### Rename (Перейменування)

Найдешевший і найцінніший рефакторинг.

```java
// ❌ До
int d = 0;       // elapsed time in days
List<Map<String, Object>> lst = getD();

// ✅ Після
int elapsedDays = 0;
List<Map<String, Object>> userReports = getUserReports();
```

### Replace Magic Number with Named Constant

```java
// ❌ До
if (user.getAge() < 18) throw new AccessDeniedException();
price = price * 0.9;

// ✅ Після
private static final int MINIMUM_AGE = 18;
private static final double DISCOUNT_RATE = 0.9;

if (user.getAge() < MINIMUM_AGE) throw new AccessDeniedException();
price = price * DISCOUNT_RATE;
```

### Introduce Parameter Object

```java
// ❌ До
public List<Flight> findFlights(
    String fromCity, String toCity, LocalDate departure,
    LocalDate returnDate, int passengers) { ... }

// ✅ Після
public List<Flight> findFlights(FlightSearchCriteria criteria) { ... }

record FlightSearchCriteria(
    String fromCity, String toCity,
    LocalDate departure, LocalDate returnDate,
    int passengers) {}
```

---

## 7. TDD як захист під час рефакторингу

TDD (Test-Driven Development) — підхід, де тест пишеться *до* коду, якого ще немає.

```
Red   → Написати тест, що падає (описати бажану поведінку)
Green → Написати мінімальний код, що робить тест зеленим
Blue  → Відрефакторити код (тест захищає від регресій)
```

Чому TDD важливий для рефакторингу: **без тестів рефакторинг сліпий**. Ви не знаєте, чи зламали щось. Тест — це сітка безпеки.

### Приклад TDD-циклу

```java
// RED: тест, що падає
@Test
void discount_applied_for_VIP_customer() {
    Order order = new Order(customer(VIP), items(100.0));
    assertThat(order.totalPrice()).isEqualTo(90.0);
}

// GREEN: мінімальна реалізація
public double totalPrice() {
    double total = items.stream().mapToDouble(Item::price).sum();
    return customer.isVip() ? total * 0.9 : total;
}

// REFACTOR: виносимо логіку знижки
public double totalPrice() {
    return applyDiscount(subtotal(), customer.discountRate());
}
```

---

## 8. Рефакторинг і Legacy Code: реальність

Legacy code — це код без тестів. Не обов'язково старий — іноді він написаний минулого місяця.

**Стратегія роботи з legacy:**

1. **Characterization Tests** — спочатку напишіть тести, що описують *поточну* поведінку (не обов'язково правильну). Це ваша сітка.
2. **Strangler Fig Pattern** — не переписуйте все одразу. Поступово замінюйте частини: нова реалізація «обмотує» стару.
3. **Seam (шов)** — знайдіть місце, де можна «вставити» тест без зміни логіки. Зазвичай — через Dependency Injection.

```java
// ❌ Нетестований legacy: статична залежність
public class OrderService {
    public void process(Order order) {
        // не можна підмінити у тесті
        PaymentGateway.charge(order.total());
        EmailSender.send(order.customer().email());
    }
}

// ✅ Після введення DI: тестовий шов з'явився
public class OrderService {
    private final PaymentGateway paymentGateway;
    private final EmailSender emailSender;

    public OrderService(PaymentGateway pg, EmailSender es) {
        this.paymentGateway = pg;
        this.emailSender = es;
    }

    public void process(Order order) {
        paymentGateway.charge(order.total());
        emailSender.send(order.customer().email());
    }
}
// Тепер можна підставити mock у тест
```

---

## 9. Інструменти підтримки рефакторингу

| Інструмент | Призначення |
| :--- | :--- |
| **IntelliJ IDEA** | Автоматичні рефакторинги: Rename, Extract Method, Inline, Move. Shift+F6, Ctrl+Alt+M |
| **SonarQube / SonarLint** | Статичний аналіз: Code Smells, Duplicates, Security Issues |
| **JaCoCo** | Вимірювання покриття тестами |
| **Checkstyle** | Перевірка стилю коду (конвенції) |
| **ArchUnit** | Архітектурні тести (залежності між пакетами) |

---

## 10. Екзаменаційний пул (Exam Questions)

**Питання 1: Дайте визначення рефакторингу за Fowler. Чим він відрізняється від оптимізації та виправлення багів?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Рефакторинг — зміна внутрішньої структури коду без зміни зовнішньої поведінки. Мета — полегшити розуміння і здешевити майбутні зміни.

Виправлення бага — змінює поведінку (бажану). Оптимізація — може змінити спостережувану поведінку (latency, throughput). Обидва не є рефакторингом.

</details>

**Питання 2: Що таке Technical Debt? Назвіть 3 причини його виникнення.**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Технічний борг — накопичені витрати на майбутні переробки через прийняті компроміси. Як фінансовий борг: нараховує «відсотки» у вигляді уповільнення розробки.

Причини: тиск дедлайнів («зробимо потім»), відсутність тестів (боїшся щось зачіпати), відсутність code review (кожен пише по-своєму), дублування коду, незрозумілі назви змінних.

</details>

**Питання 3: Що таке Code Smell? Наведіть 3 приклади з різних категорій.**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Code Smell — ознака потенційної проблеми. Не помилка, але сигнал для уважнішого погляду.

Приклади:
- `Long Method` (Bloater): метод > 30 рядків, роблячи кілька речей одразу.
- `Duplicate Code` (Dispensable): одна логіка в двох місцях — будь-яка правка потребує синхронізації.
- `Feature Envy` (Coupler): метод класу A активно використовує дані класу B — можливо, він має бути в B.

</details>

**Питання 4: Опишіть TDD-цикл Red-Green-Refactor. Як він захищає від регресій при рефакторингу?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Red: пишемо тест, що описує бажану поведінку — він падає, бо коду ще немає.
Green: пишемо мінімальний код, що робить тест зеленим.
Refactor: покращуємо код, знаючи, що тест зловить будь-яку регресію.

При рефакторингу тести — «сітка безпеки»: якщо після зміни структури код поводиться інакше — тест одразу покаже, де саме. Без тестів рефакторинг сліпий.

</details>

**Питання 5: Що таке Strangler Fig Pattern і коли він застосовується?**

<details markdown="1">
<summary>Еталонна відповідь</summary>

Strangler Fig — стратегія поступової заміни legacy-системи: нова реалізація будується паралельно і поступово «обмотує» стару, беручи на себе частини функціоналу. Як фікусовий душитель, який росте навколо дерева.

Застосовується коли: legacy-система занадто велика для одноразового переписування, бізнес потребує безперервної роботи, є ризик «великого вибуху» (Big Bang rewrite) — переписати все і нічого не запустити вчасно.

</details>

---

**[⬅️ Лекція 11: System Design](11_system_design.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
