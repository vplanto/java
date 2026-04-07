# Практикум 4: Конфігурація та Обробка Помилок (Production Ready)

**Тип:** Hands-on Lab
**Рівень:** Junior Strong
**Попередні вимоги:** Реалізований сервіс з Лабораторної №3.
**Інженерна мета:** Зрозуміти принцип "12-Factor App" (Config in environment) та навчитися професійно обробляти помилки (не показувати клієнту StackTrace).
**Бізнес-задача:** Винести курс валют у налаштування та захистити API від некоректних даних.

---

##  Експрес-опитування: Operations Check

1.  **Deployment:** Якщо ви змінили один рядок коду (наприклад, курс долара), скільки часу займає доставка цієї зміни на Production (Commit -> Build -> Deploy)?
2.  **HTTP Standards:** Чим відрізняється статус `400 Bad Request` від `500 Internal Server Error` з точки зору клієнта?
3.  **Security:** Чи безпечно зберігати пароль від бази даних у файлі `application.properties`, якщо цей файл потрапляє у GitHub?

<details markdown="1">
<summary>Відповіді (Самоперевірка)</summary>

1.  **Від 10 до 40 хвилин.** Це називається "Downtime" або "Release Cycle". Зміна конфігурації має займати секунди (перезапуск процесу), а не хвилини (перезбірка).
2.  **Виною.** `400` — винен клієнт (надіслав криві дані). Він має виправити запит. `500` — винен сервер (впала база, NPE). Клієнт нічого не може зробити, тільки чекати.
3.  **Ні.** Це витік секретів. У Git можна зберігати лише структуру конфігу або дефолтні значення для Dev-середовища. Паролі передаються через змінні оточення (Environment Variables) під час запуску.

</details>

---

## 🛑 Stop & Think: Hardcode is Evil

У вашому коді зараз є рядок:
`private static final double USD_RATE = 38.5;`

**Інженерний розбір:**
Щоб змінити курс валют, вам потрібно:
1.  Змінити Java-код.
2.  Скомпілювати проєкт.
3.  Зупинити сервер -> Залити нову версію -> Запустити сервер.

Це неприпустимо для параметрів, що часто змінюються.

---

## Крок 1. External Configuration (`application.properties`)

Spring Boot дозволяє винести налаштування у зовнішні файли або змінні середовища.

1.  Відкрийте файл `src/main/resources/application.properties`.
2.  Додайте туди бізнес-параметри:
    ```properties
    # Business Logic Config
    app.currency.usd-rate=38.5
    app.currency.commission=0.01
    ```

3.  Модифікуйте `CurrencyService`. Використовуйте анотацію `@Value`, щоб Spring "впровадив" значення з файлу.

```java
@Service
public class CurrencyService {

    private final double usdRate;
    private final double commission;

    // Spring сам знайде значення в файлі properties і підставить їх у конструктор
    public CurrencyService(
            @Value("${app.currency.usd-rate}") double usdRate,
            @Value("${app.currency.commission}") double commission) {
        this.usdRate = usdRate;
        this.commission = commission;
    }

    public double buyEuro(double uahAmount) {
        // Використовуємо this.usdRate замість хардкоду
        // ... логіка ...
    }
}

```

> **Перемога:** Тепер курс валют можна змінити навіть через змінну середовища при запуску Docker-контейнера (`--app.currency.usd-rate=40.0`), не чіпаючи код.

---

## Крок 2. Глобальна обробка помилок (Global Exception Handling)
Спробуйте відправити запит з від'ємною сумою: `/api/currency/convert?amount=-100`.
Якщо ви додали просту перевірку `throw new RuntimeException(...)`, клієнт отримає статус `500 Internal Server Error` і купу страшного тексту (Stack Trace).

**Це непрофесійно.**

* Помилка клієнта (невірні дані) — це статус `400 Bad Request`.
* JSON має бути чистим: `{"error": "Сума не може бути від'ємною"}`.

### Реалізація:
Spring має механізм "Перехоплювача помилок" — `@RestControllerAdvice`.

1. Створіть пакет `exception`.
2. Створіть клас `GlobalExceptionHandler`.

```java
package ua.edu.demoservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice // Цей клас ловить помилки з УСІХ контролерів (AOP)
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class) // Яку помилку ловимо
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException e) {
        // Формуємо красиву відповідь 400 Bad Request
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}

```

3. Оновіть `CurrencyService`, щоб він кидав саме цю помилку:
```java
public double buyEuro(double uahAmount) {
    if (uahAmount <= 0) {
        throw new IllegalArgumentException("Сума має бути більше нуля");
    }
    // ...
}

```



---

##  Завдання "На захист" (Challenge)
У вас є файл `application.properties`.
Реалізуйте механізм, де **повідомлення про помилку** також винесене в конфігурацію.

Тобто:

1. У проперті файлі: `app.messages.error.negative-amount=Не можна міняти повітря на гроші!`
2. При помилці користувач має бачити саме цей текст.

**Питання для захисту:**
Чому ми повертаємо `Map.of("error", ...)` а не просто рядок? Як це впливає на Frontend розробника?

<details markdown="1">
<summary>Відповідь</summary>

Frontend завжди очікує **JSON**.
Якщо API поверне просто рядок `"Error"`, JavaScript код `JSON.parse(response)` впаде з помилкою, бо це невалідний JSON. Об'єкт `{"error": "message"}` — це стандарт контракту.

</details>

---

##  Контрольні питання
1. **Environment Variables:** У вас є параметр `app.password` в `application.properties`. Чому комітити цей файл в Git з реальним паролем — це злочин? Як Spring дозволяє це обійти при запуску?
2. **HTTP Codes:** Ваша програма працює ідеально, але база даних впала. Який код має отримати клієнт: 200, 400 чи 500? Хто за це відповідає у вашому новому коді?
3. **Architecture:** `@RestControllerAdvice` — це частина контролера, сервісу чи окремий архітектурний патерн?

<details markdown="1">
<summary>Відповіді</summary>

1. **Security.** Паролі в Git — це витік даних. Spring дозволяє перезаписати будь-яку властивість через аргументи запуску (`java -jar app.jar --app.password=123`) або змінні середовища ОС (`export APP_PASSWORD=123`).
2. **500 Internal Server Error.** Це не вина клієнта, це вина сервера. Якщо ми не перехопили цей виняток явно, Spring Boot за замовчуванням поверне 500. Наш `GlobalExceptionHandler` повинен мати метод для `Exception.class` (general), щоб не світити деталі бази даних.
3. **AOP (Aspect Oriented Programming).** Це наскрізна логіка. Ми "вклинюємось" у процес обробки запиту, якщо сталася помилка, не змінюючи код самого контролера. Це дозволяє тримати контролери чистими.

</details>

---

**[⬅️ P04: DI та архітектура](p04_spring_architecture_di.md)** | **[P06: Cloud Deployment ➡️](p06_spring_cloud_deployment.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
