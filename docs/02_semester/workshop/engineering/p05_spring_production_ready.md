# Практикум 4: Конфігурація та Обробка Помилок (Production Ready)

**Тип:** Hands-on Lab
**Рівень:** Junior Strong
**Попередні вимоги:** Проєкт `library-service` з P04 (`BookController`, `BookService`, `BookRequest`, `BookResponse`).
**Інженерна мета:** Зрозуміти принцип "12-Factor App" (Config in environment) та навчитися професійно обробляти помилки через стандартний DTO `ApiError`.
**Бізнес-задача:** Захистити бібліотечний API від некоректних даних та стандартизувати формат відповіді при помилках.

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

## Частина 1: External Configuration (application.properties) (15 хв)

### Бізнес-сценарій: Hardcode is Evil
Уявіть, що хтось захардкодив максимальну кількість книг у бібліотеці або назву магазину прямо у код.

> [!CAUTION]
> **Чому це неприпустимо (Engineering Flaw)?**
> Щоб змінити параметр, вам потрібно:
> 1. Змінити Java-код.
> 2. Скомпілювати проєкт.
> 3. Зупинити сервер → залити нову версію → запустити сервер.
> Параметри, що змінюються, повинні бути ззовні коду.

### Завдання 1.1: Винесення конфігурації
Spring Boot дозволяє винести налаштування у зовнішні файли або змінні середовища.

**Файл: src/main/resources/application.properties**
```properties
# Business Logic Config
app.library.name=Central Library
app.library.max-books=100
```

### Завдання 1.2: Впровадження конфігурації у Service
Модифікуйте `BookService`. Використовуйте анотацію `@Value`, щоб Spring "впровадив" значення з файлу.

**Файл: src/main/java/ua/edu/libraryservice/service/BookService.java** — додайте поля:
```java
import org.springframework.beans.factory.annotation.Value;

@Service
public class BookService {

    private final String libraryName;
    private final int maxBooks;
    // ... існуючий List<BookResponse> books та AtomicLong ...

    public BookService(
            @Value("${app.library.name}") String libraryName,
            @Value("${app.library.max-books}") int maxBooks) {
        this.libraryName = libraryName;
        this.maxBooks = maxBooks;
        // seed дані з P04
    }

    public BookResponse addBook(String title, String author) {
        if (books.size() >= maxBooks) {
            throw new IllegalArgumentException(
                "Бібліотека '" + libraryName + "' заповнена (ліміт: " + maxBooks + ")");
        }
        BookResponse book = new BookResponse(idCounter.getAndIncrement(), title, author);
        books.add(book);
        return book;
    }
}
```

> [!NOTE]
> **Перемога 12-Factor App:**
> Тепер ліміт книг можна змінити через змінну середовища при запуску Docker-контейнера (`--app.library.max-books=200`), не чіпаючи код та не перекомпілюючи програму.

---

## Частина 2: Глобальна обробка помилок (Global Exception Handling) (20 хв)

### Бізнес-сценарій
Спробуйте відправити POST запит без поля `title`: `{"author": "Martin"}`. Якщо ви не обробляєте помилку, клієнт отримає `500 Internal Server Error` з купою страшного StackTrace.

> [!IMPORTANT]
> **Це непрофесійно.**
> * Помилка клієнта (невірні дані) — це статус `400 Bad Request`.
> * JSON-відповідь має бути **типізованим DTO**, а не рядком — фронтенд-розробник очікує стабільний контракт.

### Завдання 2.1: Створення `ApiError` DTO

Спочатку визначимо стандартний формат відповіді при помилках. Усі практикуми (P05, P07) використовують **один і той самий** `ApiError`.

**Файл: src/main/java/ua/edu/libraryservice/dto/ApiError.java**
```java
package ua.edu.libraryservice.dto;

import java.time.Instant;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message);
    }
}
```

### Завдання 2.2: Створення `GlobalExceptionHandler`

Spring має механізм «Перехоплювача помилок» — `@RestControllerAdvice`.

1. Створіть пакет `ua.edu.libraryservice.exception`.
2. Створіть клас `GlobalExceptionHandler`.

**Файл: src/main/java/ua/edu/libraryservice/exception/GlobalExceptionHandler.java**
```java
package ua.edu.libraryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.edu.libraryservice.dto.ApiError;

@RestControllerAdvice // Ловить помилки з УСІХ контролерів (AOP)
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiError.of(400, "Bad Request", e.getMessage()));
    }
}
```

> [!TIP]
> **ApiError vs Map:** Ми свідомо використовуємо `record ApiError`, а не `Map.of("error", ...)`. Типізований DTO гарантує стабільний контракт: фронтенд-розробник завжди знає, що отримає поля `timestamp`, `status`, `error`, `message`. `Map` цього не гарантує.

### Завдання 2.3: Оновлення `BookService`
Сервіс вже кидає `IllegalArgumentException` при переповненні (Завдання 1.2). Переконайтесь, що запит `POST /api/books` з порожнім `title` також повертає `400`, а не `500`:

```java
public BookResponse addBook(String title, String author) {
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title не може бути порожнім");
    }
    // ... решта логіки ...
}
```

---

## Частина 3: Завдання «На захист» (Challenge) (15 хв)

### Завдання: Кастомне виключення `BookNotFoundException`

1. Створіть клас `BookNotFoundException extends RuntimeException` у пакеті `exception`.
2. Додайте в `BookService` метод `findById(Long id)`, який кидає `BookNotFoundException`, якщо книга не знайдена.
3. Додайте обробник цього виключення у `GlobalExceptionHandler` — він має повертати `404 Not Found` з `ApiError`.
4. Додайте ендпоінт `GET /api/books/{id}` у `BookController`.

> [!NOTE]
> **Питання для захисту:**
> Скільки файлів треба змінити, щоб додати новий тип помилки (наприклад, `409 Conflict`)?

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

**[⬅️ P04: DI та архітектура](p04_spring_architecture_di.md)** | **[P07: Cloud Deployment ➡️](p07_spring_cloud_deployment.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
