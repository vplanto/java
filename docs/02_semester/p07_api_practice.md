# Практикум P07: API Design на практиці. Пишемо OpenAPI-специфікацію руками

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 7: API Design](07_api_design.md), базовий Spring Boot сервіс (P03)

---

## Мета заняття

Мета практикуму — пройти шлях від аудиту поганого API до написання власного працюючого Spring Boot сервісу з суворим дотриманням інженерних контрактів.

---

## Частина 1: Code Review (Інженерний аудит) (15 хв)

### Бізнес-сценарій
Вам дістався API від попередньої команди (Legacy). Перш ніж писати новий бекенд на Spring Boot, ви маєте провести інженерний аудит та знайти архітектурні помилки у їхніх URL та HTTP-статусах. Це критично для проєктування правильного контракту майбутньої системи.

### Вправа 1.1: Знайди помилки в URL
Команда-попередник запропонувала такий API для управління книгами. Знайдіть щонайменше 5 проблем, які порушують REST-принципи:

```
POST   /getAllBooks
POST   /createNewBook
GET    /book/delete?id=42
POST   /books/update
GET    /getBookById/42
PUT    /books/42/doPublish
GET    /Books/list
```

<details markdown="1">
<summary>Еталон аудиту (URL)</summary>

| Проблема | Приклад з умови | Правильно |
|---|---|---|
| Дієслово в URL | `/getAllBooks`, `/createNewBook` | `GET /books`, `POST /books` |
| DELETE через GET з параметром | `GET /book/delete?id=42` | `DELETE /books/42` |
| Непослідовне використання множини | `/book/delete` vs `/books` | Завжди множина: `/books` |
| Непотрібне дієслово у дії | `PUT /books/42/doPublish` | `PATCH /books/42` з body `{"status": "PUBLISHED"}` |
| Непослідовний регістр | `/Books/list` | Тільки lowercase: `/books` |

</details>

### Вправа 1.2: Аналіз HTTP-статусів
Яку відповідь повертає Legacy-система і що в цьому неправильно? Проаналізуйте сценарії:

| Сценарій | Поточна відповідь (Legacy) | Правильний статус-код |
|---|---|---|
| Книгу успішно видалено | `200 OK` з `{"message": "deleted"}` | `204 No Content` |
| Книги з ID=999 не існує | `200 OK` з `{"data": null}` | `404 Not Found` |
| Зробив POST, книгу створено | `200 OK` | `201 Created` |
| Читач не авторизований | `403 Forbidden` | `401 Unauthorized` |
| Поле `title` відсутнє у запиті | `500 Internal Server Error` | `400 Bad Request` |

<details markdown="1">
<summary>Обґрунтування (Статуси)</summary>

- **204 vs 200**: DELETE не має повертати тіло, якщо ресурс видалено完全に.
- **404 vs 200**: 200 означає "успішно", а відсутність ресурсу — це помилка клієнта.
- **201 vs 200**: Створення ресурсу має свій специфічний статус.
- **401 vs 403**: 401 — "я не знаю хто ти", 403 — "я знаю хто ти, але не пущу".
- **400 vs 500**: Помилка валідації запиту — це завжди провина клієнта (4xx).

</details>

### Архітектурне рішення (ADR): Від аудиту до імплементації

Оскільки під час аудиту (Частина 1) ми виявили фундаментальні архітектурні вади у Legacy-системі — від порушення чистоти URL до маскування помилок клієнта статусом 200/500 — ми приймаємо стратегічне рішення: **не рефакторити старий код, а написати новий сервіс з нуля на Spring Boot**.

Ми будемо використовувати **Contract-First підхід**:
- Всі виправлення, які ви занесли в таблиці аудиту, стають нашими новими **Non-Functional Requirements (NFR)**.
- Це гарантує, що нова система буде безпечною, стабільною та зручною для фронтенд-розробників із самого початку.

---

## Частина 2: Ініціалізація проєкту та Інженерні контракти (20 хв)

### 2.1: Створення Maven-проєкту

Для початку роботи необхідно ініціалізувати Maven-проєкт (Java 17 або 21). Ви можете скористатися [Spring Initializr](https://start.spring.io/) або створити `pom.xml` вручну.

**Потрібні залежності:**
- `spring-boot-starter-web` — для створення REST-контролерів.
- `springdoc-openapi-starter-webmvc-ui` — для автогенерації OpenAPI специфікації та доступу до Swagger UI.
- `spring-boot-starter-test` — для написання модульних та інтеграційних тестів.

```xml
<project>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.4</version>
        <relativePath/>
    </parent>

    <dependencies>
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- OpenAPI/Swagger documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.5</version>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```


### 2.1.1: Створення через IntelliJ IDEA (По кроках)

Якщо ви використовуєте IntelliJ IDEA Ultimate або Community з плагіном Spring, це найшвидший шлях:

1. **Запустіть Майстер:** `File` -> `New` -> `Project...`
2. **Оберіть Spring Initializr:** У лівій панелі оберіть **Spring Initializr**.
3. **Налаштуйте Проєкт:**
   - **Name:** `library`
   - **Group:** `ua.edu.onu`
   - **Artifact:** `library`
   - **Package name:** `ua.edu.onu.library`
   - **Build system:** `Maven`
   - **JDK:** 17 або 21
4. **Оберіть залежності (Dependencies):**
   - Наступний крок (Next).
   - У пошуку знайдіть та додайте:
     - `Spring Web`
     - `SpringDoc OpenAPI UI` (або просто `springdoc` в останніх версіях)
5. **Фініш:** Натисніть **Create**. IDEA сама згенерує `pom.xml` та структуру папок.


### 2.2: Інженерні контракти (NFR)

Щоб API було професійним та підтримуваним, ми фіксуємо наступні правила (Non-Functional Requirements):

1.  **Immutability:** Для передачі даних (DTO) використовуємо Java `record`.
2.  **Versioning:** Версія API обов'язково вказується в URL (наприклад, `/api/v1/books`).
3.  **Security Contract:** Всі ендпоінти очікують обов'язковий заголовок `X-Auth-Token`. На рівні контракту ми валідуємо його наявність.

### 2.3: Проєктування DTO (Domain Model)

Тепер, коли проєкт налаштований, необхідно створити модель даних для нашого сервісу "Бібліотека". Ваші DTO мають відповідати бізнес-моделі, яку ви спроєктували під час аудиту.

**Завдання:** Створіть у пакеті `ua.edu.onu.library.dto` (директорія `src/main/java/ua/edu/onu/library/dto/`) класи `record` для наступних сутностей.

**Файл: src/main/java/ua/edu/onu/library/dto/Book.java**
```java
package ua.edu.onu.library.dto;

import java.time.LocalDate;

/**
 * DTO для представлення книги в системі. 
 */
public record Book(Long id, String title, String isbn, LocalDate publishedAt) {}
```

**Файл: src/main/java/ua/edu/onu/library/dto/Loan.java**
```java
package ua.edu.onu.library.dto;

import java.time.LocalDate;

public record Loan(Long id, Long bookId, Long readerId, LocalDate dueDate) {}
```

**Файл: src/main/java/ua/edu/onu/library/dto/LoanRequest.java**
```java
package ua.edu.onu.library.dto;

public record LoanRequest(Long bookId, Long readerId) {}
```

**Важливо:** Продумайте поля для кожної сутності. Структура цих об'єктів — це ваш контракт із клієнтом.

> [!IMPORTANT]
> **Vibe Coding Protocol:** Ви можете використовувати ШІ (ChatGPT/Copilot) для генерації "каркасу" ваших DTO. Але ваша відповідальність як інженера — перевірити, щоб структура була не просто синтаксично правильною, а відповідала бізнес-вимогам бібліотеки (наприклад, наявність ISBN або dueDate).

---

## Частина 3: Реалізація розгалуженого API (25 хв)

### 3.1: Створення LibraryController

Тепер ми переходимо до написання коду, виправляючи помилки, які ми знайшли під час аудиту. Наш контролер повинен суворо дотримуватися REST-контрактів та повертати коректні HTTP-статуси.

> [!CAUTION]
> **Vibe Coding Protocol (AI Ownership):** Ви можете попросити AI згенерувати код `LibraryController`. АЛЕ ваша зона відповідальності — переконатися, що згенерований код суворо відповідає правильним HTTP-статусам з Частини 1. Якщо AI за звичкою запропонує `200 OK` для видалення ресурсу — ви маєте змусити його виправити це на `204 No Content`.

**Відкрийте файл src/main/java/ua/edu/onu/library/controller/LibraryController.java та імплементуйте наступне:**

```java
package ua.edu.onu.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.edu.onu.library.dto.Book;
import ua.edu.onu.library.dto.Loan;
import ua.edu.onu.library.dto.LoanRequest;
import ua.edu.onu.library.exception.UnauthorizedException;
import ua.edu.onu.library.service.LibraryService;

@RestController
@RequestMapping("/api/v1")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    // 1. Отримання книги — якщо немає, кидаємо Exception (буде 404)
    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable Long id, @RequestHeader("X-Auth-Token") String token) {
        validateAuth(token);
        return libraryService.findBookById(id);
    }

    // 2. Створення позики — повертаємо 201 Created
    @PostMapping("/loans")
    public ResponseEntity<Loan> createLoan(@RequestBody LoanRequest request, @RequestHeader("X-Auth-Token") String token) {
        validateAuth(token);
        Loan loan = libraryService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    // 3. Видалення книги — повертаємо 204 No Content
    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id, @RequestHeader("X-Auth-Token") String token) {
        validateAuth(token);
        libraryService.deleteBook(id);
    }

    private void validateAuth(String token) {
        if (token == null || !token.equals("test-token")) {
            throw new UnauthorizedException("Missing or invalid X-Auth-Token");
        }
    }
}
```

### 3.2: Кастомні виключення та сервіси

Для коректної роботи нам потрібні класи виключень, які Spring зможе перехопити та перетворити на зрозумілі відповіді.

**Створіть у пакеті ua.edu.onu.library.exception наступні виключення:**

**Файл: src/main/java/ua/edu/onu/library/exception/UnauthorizedException.java**
```java
package ua.edu.onu.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
```

**Файл: src/main/java/ua/edu/onu/library/exception/BookNotFoundException.java**
```java
package ua.edu.onu.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) { super(message); }
}
```

**Тепер реалізуйте заглушку сервісу у файлі src/main/java/ua/edu/onu/library/service/LibraryService.java:**

```java
package ua.edu.onu.library.service;

import org.springframework.stereotype.Service;
import ua.edu.onu.library.dto.Book;
import ua.edu.onu.library.dto.Loan;
import ua.edu.onu.library.dto.LoanRequest;
import ua.edu.onu.library.exception.BookNotFoundException;
import java.time.LocalDate;

@Service
public class LibraryService {
    public Book findBookById(Long id) {
        if (id == 999) throw new BookNotFoundException("Book with id 999 not found");
        return new Book(id, "Clean Code", "978-0132350884", LocalDate.now());
    }

    public Loan createLoan(LoanRequest request) {
        return new Loan(1L, request.bookId(), request.readerId(), LocalDate.now().plusDays(14));
    }

    public void deleteBook(Long id) {
        // Логіка видалення
    }
}
```


---

---

## Частина 4: Глобальна обробка помилок (RestControllerAdvice) (15 хв)

Пам'ятаєте таблицю з **Частини 1**, де Legacy-система повертала `500` на відсутнє поле або `200` на неавторизований запит? Саме в цьому розділі ми виправимо ці архітектурні помилки. Механізм `@RestControllerAdvice` дозволяє нам централізовано перетворювати будь-яку виняткову ситуацію на коректний HTTP-статус.

### 4.1: Контракт помилки (ApiError)

Для кожного запиту, що завершився невдачею, клієнт повинен отримати об'єкт із чітко визначеними полями. Ми використовуємо `record`, щоб гарантувати незмінність даних та спростити код.

**Створіть файл src/main/java/ua/edu/onu/library/dto/ApiError.java:**

```java
package ua.edu.onu.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import java.time.Instant;

public record ApiError(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );
    }
}
```

### 4.2: Центр управління виключеннями

Клас із анотацією `@RestControllerAdvice` дозволяє перехоплювати виключення по всьому проєкту в одному місці та повертати коректні статуси.

**Створіть файл src/main/java/ua/edu/onu/library/controller/GlobalExceptionHandler.java:**

```java
package ua.edu.onu.library.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.edu.onu.library.dto.ApiError;
import ua.edu.onu.library.exception.BookNotFoundException;
import ua.edu.onu.library.exception.UnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Виправляємо Legacy: замість 500 на невалідні дані повертаємо 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 2. Виправляємо Legacy: повертаємо чітке 401 Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // 3. Обробка відсутності ресурсу (404)
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            BookNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 4. Обробка відсутності обов'язкових заголовків (400)
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Контракт порушено: " + ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = ApiError.of(status, message, request.getRequestURI());
        return new ResponseEntity<>(error, status);
    }
}
```

**Порада:** Виправлення помилки "500 на відсутнє поле" робиться саме тут через перехоплення `MethodArgumentNotValidException` або `IllegalArgumentException`.

---

## Частина 5: Автогенерація OpenAPI та тестування через Insomnia (15 хв)

Ми відходимо від ручного написання YAML-файлів. Завдяки залежності `springdoc-openapi`, наш сервіс сам генерує документацію на основі коду.

### 5.1: Генерація OpenAPI документації

Для отримання актуальної специфікації виконайте наступне:

1.  **Запустіть додаток:** Виконайте `mvn spring-boot:run` або запустіть `Application.java` через IDE.
2.  **Отримайте JSON:** Відкрийте у браузері [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs). Ви побачите згенерований JSON-файл, що описує ваші контролери та схеми (наприклад, `Book` та `ApiError`).
3.  **Swagger UI:** Ви також можете переглянути документацію в інтерактивному режимі за адресою [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

#### 5.1.1: Тестування через Swagger UI (Try it out)

Swagger — це не просто документація, а жива пісочниця для вашого API:

1. **Відкрийте Swagger UI** за адресою вище.
2. **Оберіть ендпоінт:** Наприклад, розгорніть секцію `GET /api/v1/books/{id}`.
3. **Try it out:** Натисніть кнопку **"Try it out"** у верхньому правому куті блоку. Це розблокує поля для введення.
4. **Введіть дані:**
   - У поле `id` введіть будь-яке число (наприклад, `1`).
   - У поле `X-Auth-Token` введіть `test-token`.
5. **Execute:** Натисніть синю кнопку **"Execute"**.
6. **Аналіз відповіді:**
   - Спустіться нижче до розділу **Responses**.
   - Ви побачите **Server response** (код 200) та тіло JSON з даними про книгу.
   - Спробуйте прибрати заголовок `X-Auth-Token` або змінити його на неправильний та подивіться, як змінюється відповідь та JSON-контракт помилки (`ApiError`).

> [!TIP]
> **Чому я отримую 400 замість 401?**
> Якщо ви не вказали заголовок `X-Auth-Token` взагалі, Spring поверне `400 Bad Request` (відсутній обов'язковий параметр запиту). 
> Якщо ви вказали заголовок, але він неправильний, спрацює наш метод `validateAuth` і ви отримаєте `401 Unauthorized`.


### 5.2: Тестування в Insomnia

[Insomnia](https://insomnia.rest/) — це сучасний HTTP-клієнт, який ідеально підходить для тестування REST API.

1.  **Імпорт:** Створіть нову колекцію та імпортуйте JSON, отриманий на попередньому кроці. Insomnia автоматично створить запити для всіх ваших ендпоінтів.
2.  **Тест без авторизації:** Виконайте `GET /api/v1/books/1`.     *   **Очікуваний результат:** `401 Unauthorized`.
    *   **Тіло відповіді:** об'єкт `ApiError` з вашим повідомленням "Missing or invalid X-Auth-Token".
3.  **Тест з авторизацією:**
    *   Перейдіть на вкладку **Header** у запиті.
    *   Додайте новий заголовок:
        *   **Name:** `X-Auth-Token`
        *   **Value:** `test-token`
    *   Виконайте запит знову.
    *   **Очікуваний результат:** `200 OK`.

4.  **Створення позики (POST /loans):**
    *   Оберіть запит `POST /api/v1/loans`.
    *   У вкладці **Body** оберіть тип **JSON**.
    *   Введіть тіло запиту:
        ```json
        {
          "bookId": 1,
          "readerId": 42
        }
        ```
    *   Не забудьте додати заголовок `X-Auth-Token: test-token` у вкладці **Header**.
    *   **Очікуваний результат:** `201 Created` та об'єкт `Loan` у відповіді.

5.  **Видалення книги (DELETE /books/{id}):**
    *   Оберіть запит `DELETE /api/v1/books/1`.
    *   Додайте заголовок `X-Auth-Token: test-token`.
    *   **Очікуваний результат:** `204 No Content` (порожня відповідь).


### 5.3: Вправа: Валідація Breaking Changes

Зміна контракту без зміни версії API — одна з найчастіших причин поломок додатків у продакшені. Проведімо експеримент.

**Завдання:** 
1.  Змініть у своєму Java-коді (record `Book`) тип поля `id` з `Long` на `String` АБО перейменуйте поле `title` на `bookTitle`.
2.  Перезапустіть додаток та згенеруйте OpenAPI JSON знову.
3.  Порівняйте новий JSON із попереднім.

**Питання для аналізу:**
- Як ця зміна вплине на мобільний додаток, який вже використовує ваш API і очікує старий формат?
- Виходячи з таблиці нижче, чи є ваша зміна "Breaking Change"?

| Зміна | Breaking? | Чому |
|---|---|---|
| Нове поле у відповіді | ❌ Ні | Клієнти зазвичай ігнорують невідомі поля |
| Перейменування поля | ✅ Так | Клієнт не знайде потрібний ключ у JSON |
| Зміна типу (int -> string) | ✅ Так | Парсер клієнта зламається при десеріалізації |
| Зміна обов'язкового поля | ✅ Так | Клієнт не надішле нове поле -> 400 Bad Request |

---

## Частина 6: Фіксація контрактів через автотести (@WebMvcTest) (20 хв)

Ручне тестування в Insomnia довело, що наш код працює "тут і зараз". Але щоб він не зламався завтра при рефакторингу, бізнес-правила (контракти) мають бути зафіксовані автотестами.

### 6.1: Налаштування тесту

Ми протестуємо наш контролер, не запускаючи весь додаток, за допомогою `@WebMvcTest`. Ця анотація піднімає лише веб-шар (slices), що робить тести миттєвими. Для ізоляції від логіки сервісів ми використаємо `@MockBean`.

> [!NOTE]
> **Інженерна довідка по імпортах:**
> *   **`@WebMvcTest`** та **`MockMvc`** — дозволяють тестувати контролери в ізоляції (без БД та важких сервісів).
> *   **`@MockBean`** — створює "фейковий" (Mock) об'єкт та реєструє його в контексті Spring. Це дозволяє нам точно керувати поведінкою сервісів (наприклад, змусити сервіс кинути помилку).
> *   **Статичні імпорти (`import static`)** — необхідні для використання таких методів як `when()`, `get()`, `status()`, `jsonPath()`. Вони роблять код тесту читабельним (Fluent API).


**Створіть файл src/test/java/ua/edu/onu/library/controller/LibraryControllerTest.java:**

```java
package ua.edu.onu.library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.onu.library.dto.Book;
import ua.edu.onu.library.exception.BookNotFoundException;
import ua.edu.onu.library.service.LibraryService;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryController.class)
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibraryService libraryService;

    @Test
    void getBook_WhenExists_ReturnsOk() throws Exception {
        // Given
        Long bookId = 1L;
        Book book = new Book(bookId, "Clean Code", "978-0132350884", LocalDate.now());
        when(libraryService.findBookById(bookId)).thenReturn(book);

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                .header("X-Auth-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void getBook_WhenNotFound_Returns404() throws Exception {
        // 1. Given: сервіс кидає нашу кастомну помилку
        Long bookId = 999L;
        when(libraryService.findBookById(bookId))
            .thenThrow(new BookNotFoundException("Book not found"));

        // 2. When & Then: перевіряємо, що GlobalExceptionHandler правильно мапить її на 404, а не 500
        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                .header("X-Auth-Token", "test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getBook_WithoutHeader_Returns400() throws Exception {
        // Перевіряємо, що відсутність обов'язкового заголовка — це помилка клієнта (400)
        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getBook_WithInvalidHeader_Returns401() throws Exception {
        // Перевіряємо, що неправильний токен — це помилка авторизації (401)
        mockMvc.perform(get("/api/v1/books/1")
                .header("X-Auth-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
```

### 6.2: Що тут відбувається?

1.  **`@WebMvcTest(LibraryController.class)`:** Spring завантажує лише веб-контекст. Це працює набагато швидше, ніж повний запуск додатку.
2.  **`MockMvc`:** Основний інструмент для надсилання HTTP-запитів до контролера в тестах.
3.  **`@MockBean`:** Ми створюємо "фейковий" об'єкт сервісу, щоб протестувати лише логіку контролера (мапінг статусів, заголовки).
4.  **`jsonPath`:** Дозволяє перевіряти структуру JSON-відповіді (наш `ApiError` контракт).

---

## Контрольні питання

1. **Breaking change:** Ви змінюєте поле `dueDate` у відповіді Loan з формату `"2024-03-15"` (date) на `"2024-03-15T00:00:00Z"` (datetime). Це breaking change?

<details markdown="1">
<summary>Відповідь</summary>

Так, це breaking change. Хоча дата та сама, string-парсери клієнта налаштовані на конкретний формат. JSON-десеріалізатор, що очікував `LocalDate`, отримає рядок з часом і впаде. Потрібна версія API або migrate-period.

</details>

2. **Idempotency:** Клієнт відправив `DELETE /books/42` і отримав timeout. Він відправляє запит знову. Яка правильна поведінка сервера при повторному DELETE до вже видаленого ресурсу?

<details markdown="1">
<summary>Відповідь</summary>

Правильна відповідь — `404 Not Found`. DELETE є idempotent: повторні виклики мають той самий ефект (ресурс видалено). Але це не означає, що статус-код має бути однаковим — при першому виклику `204 No Content`, при повторному `404`. Клієнт повинен обробляти обидва як «операція успішна».

</details>

3. **Тестування:** Чому для тестування API-контролера ми використовуємо `@WebMvcTest`, а не `@SpringBootTest`? Як це впливає на швидкість виконання тестів у CI/CD?

<details markdown="1">
<summary>Відповідь</summary>

- **Швидкість:** `@WebMvcTest` — це "slice testing", він завантажує лише веб-шар (контролери, фільтри, поради). `@SpringBootTest` завантажує весь контекст додатку (включаючи БД, черги, всі сервіси), що значно повільніше.
- **CI/CD:** Використання легких тестів дозволяє розробникам отримувати фідбек за секунди, а не хвилини. Це критично для швидкого проходження пайплайнів та економії ресурсів серверів збірки.
- **Ізоляція:** `@WebMvcTest` змушує нас мокувати залежності, що робить тест справжнім Unit-тестом веб-шару, не залежним від багів у сервісах чи БД.

</details>

4. **Захист від регресії:** Як написання автотесту з `@WebMvcTest` допомагає захистити наш API від регресії (повернення старих архітектурних помилок, які ми знайшли на початку заняття)?

<details markdown="1">
<summary>Відповідь</summary>

Автотест фіксує **REST-контракт**. Якщо хтось у майбутньому випадково змінить статус-код видалення назад на `200 OK` або прибере обов'язковий заголовок авторизації, тест миттєво "впаде" у CI/CD. Це гарантує, що інженерні рішення, прийняті сьогодні, не будуть втрачені завтра через неуважність або "звичку" ШІ повертати стандартні статус-коди.

</details>

---

**[⬅️ Лекція 7: API Design](07_api_design.md)** | **[P08: Testing Practice ➡️](p08_testing_practice.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
