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

1.  **Довго (хвилини або навіть години).** Повний цикл збірки та деплою є ресурсомістким. Саме тому бізнес-параметри (наприклад, курс валют) не можна "хардкодити" в коді — їх треба виносити в конфігурацію, щоб змінювати миттєво без перезбірки всього проєкту.
2.  **Джерелом помилки (тим, хто винен).** `400` означає, що клієнт надіслав некоректні дані, і саме він має виправити запит. `500` означає, що проблема виникла на боці сервера (наприклад, необроблений виняток), і клієнт може лише зачекати, поки розробники це полагодять.
3.  **Абсолютно ні.** Це прямий шлях до витоку секретів (Credential Leak), оскільки історія Git зберігається назавжди і може бути скомпрометована. Бойові паролі повинні передаватися безпечно, наприклад, через змінні оточення (Environment Variables) під час запуску програми.

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

> [!TIP]
> **Довідка: Що таке 12-Factor App?**
> Це набір з 12 правил (методологія) для створення надійних сучасних веб-застосунків (Cloud-Native). Її розробили інженери платформи **Heroku** — піонери хмарного хостингу (PaaS), які відомі тим, що першими у світі зробили деплой застосунків магічно простим за допомогою однієї команди (`git push heroku master`).
> Одне з найголовніших правил — **Фактор 3: Конфігурація (Config)**. Воно стверджує: *«Зберігайте конфігурацію у середовищі виконання»*. Суть у тому, що код (ваші `*.java` файли) має залишатися незмінним для всіх середовищ, а всі параметри (назви баз даних, ліміти, ключі доступу) повинні завантажуватись динамічно під час запуску.

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

**Файл: src/main/java/ua/edu/libraryservice/service/BookService.java** — оновіть весь файл:
```java
package ua.edu.libraryservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.edu.libraryservice.dto.BookResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookService {

    private final String libraryName;
    private final int maxBooks;
    
    private final List<BookResponse> books = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    // Spring автоматично підставить значення з application.properties
    public BookService(
            @Value("${app.library.name}") String libraryName,
            @Value("${app.library.max-books}") int maxBooks) {
        this.libraryName = libraryName;
        this.maxBooks = maxBooks;
        
        // Початкові дані (seed)
        books.add(new BookResponse(idCounter.getAndIncrement(), "Clean Code", "Robert C. Martin"));
        books.add(new BookResponse(idCounter.getAndIncrement(), "The Pragmatic Programmer", "David Thomas"));
    }

    public List<BookResponse> findAll() {
        return List.copyOf(books);
    }

    public BookResponse addBook(String title, String author) {
        // Використовуємо конфігураційні параметри
        if (books.size() >= maxBooks) {
            throw new IllegalArgumentException(
                "Бібліотека '" + libraryName + "' заповнена (ліміт: " + maxBooks + ")");
        }
        BookResponse book = new BookResponse(idCounter.getAndIncrement(), title, author);
        books.add(book);
        return book;
    }

    public List<BookResponse> findByAuthor(String author) {
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author must not be blank");
        }
        return books.stream()
                .filter(b -> b.author().equalsIgnoreCase(author))
                .toList();
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

@RestControllerAdvice // Глобальний "перехоплювач" помилок для всіх контролерів
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
> **Чому ми створили `ApiError`?**
> Ми свідомо використовуємо типізований об'єкт `record ApiError` (DTO) для обробки помилок. Це створює надійний контракт (API Contract): фронтенд-розробники завжди точно знають, що у разі будь-якої помилки вони гарантовано отримають JSON з визначеними полями `timestamp`, `status`, `error` та `message`.

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

## Частина 3: Розбір коду — Кастомне виключення (15 хв)

### Бізнес-сценарій
Що робити, якщо клієнт запитує книгу за ID, якого не існує? Замість того, щоб повертати `500 Internal Server Error` або порожню відповідь, наш API має повернути чіткий статус `404 Not Found` та зрозумілу помилку.
Давайте розберемо, як це реалізувати за допомогою кастомного виключення (Custom Exception) та `GlobalExceptionHandler`.

### Крок 1: Створення кастомного виключення
Створимо власне виключення, яке буде чітко сигналізувати про відсутність книги.

**Файл: src/main/java/ua/edu/libraryservice/exception/BookNotFoundException.java**
```java
package ua.edu.libraryservice.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}
```

### Крок 2: Логіка пошуку в `BookService`
Додамо метод `findById`, який шукає книгу. Якщо її немає — навмисно кидаємо наше нове виключення:

```java
    // Метод у BookService.java
    public BookResponse findById(Long id) {
        return books.stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException("Книгу з ID " + id + " не знайдено"));
    }
```

### Крок 3: Обробка виключення в `GlobalExceptionHandler`
Тепер навчимо наш глобальний обробник перехоплювати `BookNotFoundException` і перетворювати його на правильну HTTP відповідь `404 Not Found`:

```java
    // Метод у GlobalExceptionHandler.java
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BookNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // Повертаємо 404
                .body(ApiError.of(404, "Not Found", e.getMessage()));
    }
```

### Крок 4: Ендпоінт у `BookController`
Контролер просто викликає сервіс. Він не містить жодного `try-catch`, оскільки знає, що у разі помилки `GlobalExceptionHandler` перехопить її:

```java
    // Метод у BookController.java
    @GetMapping("/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return bookService.findById(id);
    }
```

> [!NOTE]
> **Архітектурне питання:**
> Якщо нам знадобиться обробити інший тип помилки (наприклад, `409 Conflict`, якщо книга вже існує), скільки файлів нам доведеться змінити?

<details markdown="1">
<summary>Відповідь</summary>

**Три файли:**
1. Створити клас `DuplicateBookException.java`.
2. Додати перевірку та `throw new DuplicateBookException()` у `BookService`.
3. Додати новий метод з `@ExceptionHandler` у `GlobalExceptionHandler`.
*Контролер при цьому не змінюється взагалі!*
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
3. **Окремий архітектурний рівень (Глобальний перехоплювач).** Він діє як "фільтр", що стоїть між контролером та клієнтом. Ми виносимо логіку обробки помилок в одне місце, замість того щоб дублювати `try-catch` у кожному методі. Це дозволяє тримати контролери абсолютно чистими.

</details>

---

**[⬅️ P04: DI та архітектура](p04_spring_architecture_di.md)** | **[P07: Cloud Deployment ➡️](p07_spring_cloud_deployment.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
