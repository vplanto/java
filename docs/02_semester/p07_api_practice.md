# Практикум P07: API Design на практиці. Пишемо OpenAPI-специфікацію руками

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 7: API Design](07_api_design.md), базовий Spring Boot сервіс (P03)

> **English version:** [English](en/p07_api_practice.md)

---

## Мета заняття

Спроєктувати REST API для сервісу «Бібліотека» — від URL-структури до OpenAPI YAML-файлу. Перевірити через Postman і задокументувати breaking changes.

---

## Частина 1: Проєктування URL-структури (20 хв)

### Вправа 1.1: Знайди помилки

Команда запропонувала такий API для управління книгами. Знайдіть щонайменше 5 проблем:

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
<summary>Відповідь</summary>

| Проблема | Приклад з умови | Правильно |
|---|---|---|
| Дієслово в URL | `/getAllBooks`, `/createNewBook` | `GET /books`, `POST /books` |
| DELETE через GET з параметром | `GET /book/delete?id=42` | `DELETE /books/42` |
| Непослідовне використання множини | `/book/delete` vs `/books` | Завжди множина: `/books` |
| Непотрібне дієслово у дії | `PUT /books/42/doPublish` | `PATCH /books/42` з body `{"status": "PUBLISHED"}` |
| Непослідовний регістр | `/Books/list` | Тільки lowercase: `/books` |

</details>

### Вправа 1.2: Спроєктуй сам

Спроєктуйте API для сервісу «Бібліотека». Є сутності: `Book`, `Author`, `Reader`, `Loan` (позика книги).

Вимоги:
- Читач може взяти книгу (`Loan`)
- Книга може бути написана кількома авторами
- Потрібна можливість отримати всі книги конкретного автора
- Потрібна можливість подовжити термін позики

<details markdown="1">
<summary>Еталонний дизайн</summary>

```
# Books
GET    /books                      → список книг (з фільтрацією: ?author=42&genre=fiction)
POST   /books                      → додати книгу
GET    /books/{id}                 → деталі книги
PUT    /books/{id}                 → повне оновлення
PATCH  /books/{id}                 → часткове оновлення (наприклад, змінити статус)
DELETE /books/{id}                 → видалити книгу

# Authors — вкладений ресурс для зв'язку
GET    /authors/{id}/books         → всі книги автора
POST   /books/{id}/authors         → додати автора до книги

# Loans
POST   /loans                      → взяти книгу (body: {bookId, readerId, dueDate})
GET    /loans/{id}                 → деталі позики
GET    /readers/{id}/loans         → всі позики читача

# Extend — дія над ресурсом, де немає чистого CRUD-відповідника
POST   /loans/{id}/extend          → подовжити позику (тут POST на subresouce — прийнятно)
```

</details>

---

## Частина 2: HTTP-статуси і помилки (15 хв)

### Вправа 2.1: Правильний статус-код

Яку відповідь поверне API і чому?

| Сценарій | Поточна відповідь в коді | Правильно? |
|---|---|---|
| Книгу успішно видалено | `200 OK` з `{"message": "deleted"}` | |
| Книги з ID=999 не існує | `200 OK` з `{"data": null}` | |
| Зробив POST, книгу створено | `200 OK` | |
| Читач не авторизований | `403 Forbidden` | |
| Поле `title` відсутнє у запиті | `500 Internal Server Error` | |

<details markdown="1">
<summary>Відповідь</summary>

| Сценарій | Поточна відповідь | Правильно |
|---|---|---|
| Видалено | `200 OK` | `204 No Content` — немає тіла відповіді |
| Не знайдено | `200 OK` з null | `404 Not Found` — 200 означає «є, але порожньо» |
| Створено | `200 OK` | `201 Created` з заголовком `Location: /books/42` |
| Не авторизований | `403 Forbidden` | `401 Unauthorized` — 403 = знаю хто ти, але не дозволяю |
| Невалідні дані | `500 Internal Server Error` | `400 Bad Request` — це помилка клієнта, не сервера |

</details>

### Вправа 2.2: Уніфікований формат помилок

Напишіть Java-клас `ApiError`, який Spring буде повертати при будь-якій помилці:

Вимоги:
- Поля: `timestamp`, `status` (число), `error` (текст статусу), `message` (деталі), `path`
- Ніякого stack trace у відповіді

<details markdown="1">
<summary>Реалізація</summary>

```java
public record ApiError(
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            BookNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }
}
```

Приклад відповіді при 404:
```json
{
  "timestamp": "2024-03-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book with id 999 not found",
  "path": "/books/999"
}
```

</details>

---

## Частина 3: OpenAPI специфікація (25 хв)

### Вправа 3.1: Написати YAML для ендпоінту

Напишіть OpenAPI 3.0 специфікацію для `GET /books/{id}`. Специфікація має описати:
- Параметр `id`
- Схему відповіді `Book`
- Коди 200 і 404

<details markdown="1">
<summary>Еталон</summary>

```yaml
openapi: "3.0.3"
info:
  title: Library API
  version: "1.0"

paths:
  /books/{id}:
    get:
      summary: Get book by ID
      tags: [Books]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
          description: Unique book identifier
      responses:
        "200":
          description: Book found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
        "404":
          description: Book not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'

components:
  schemas:
    Book:
      type: object
      required: [id, title, isbn]
      properties:
        id:
          type: integer
          format: int64
          example: 42
        title:
          type: string
          example: "Clean Code"
        isbn:
          type: string
          pattern: '^\d{3}-\d{10}$'
          example: "978-0132350884"
        publishedAt:
          type: string
          format: date
          example: "2008-08-11"

    ApiError:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
        error:
          type: string
        message:
          type: string
        path:
          type: string
```

</details>

### Вправа 3.2: Breaking Change аналіз

Є v1 API і v2. Яка зміна є breaking change, яка — ні?

| Зміна | Breaking? |
|---|---|
| Додали нове поле `rating` до відповіді `Book` | |
| Перейменували поле `author` → `authorName` | |
| Змінили тип поля `id` з `string` на `integer` | |
| Видалили необов'язкове поле `description` | |
| Зробили поле `isbn` обов'язковим у запиті (було необов'язковим) | |
| Додали новий необов'язковий параметр `?lang=uk` | |

<details markdown="1">
<summary>Відповідь</summary>

| Зміна | Breaking? | Чому |
|---|---|---|
| Нове поле `rating` у відповіді | ❌ Ні | Клієнти ігнорують невідомі поля |
| Перейменування `author` → `authorName` | ✅ Так | Клієнти очікують поле `author` — воно зникне |
| Тип `id`: string → integer | ✅ Так | Парсер клієнта зламається |
| Видалення необов'язкового поля | ✅ Так | Якщо клієнт покладається на це поле |
| `isbn` стало обов'язковим | ✅ Так | Старі запити без `isbn` отримають 400 |
| Новий необов'язковий параметр | ❌ Ні | Старі клієнти просто не передають його |

</details>

---

## Частина 4: Тестування API через Postman (10 хв)

Для запущеного Spring Boot сервісу:

1. Протестуйте happy path: `POST /books` → `GET /books/{id}` → `DELETE /books/{id}`.
2. Перевірте негативні сценарії: запит з відсутнім полем, запит до неіснуючого ID.
3. Збережіть Collection та напишіть Postman Tests (JavaScript):

```javascript
// Тест після POST /books
pm.test("Status is 201", () => {
    pm.response.to.have.status(201);
});
pm.test("Location header present", () => {
    pm.response.to.have.header("Location");
});
pm.test("Response has id", () => {
    const book = pm.response.json();
    pm.expect(book.id).to.be.a('number');
    pm.environment.set("bookId", book.id);
});
```

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

3. **Версіонування:** Ваш API використовує URL-версіонування (`/v1/books`). Команда пропонує перейти на Header-версіонування (`Accept: application/vnd.api.v1+json`). Які trade-offs?

<details markdown="1">
<summary>Відповідь</summary>

URL-версіонування: просте для кешування (CDN кешує per URL), просте для тестування (можна відкрити в браузері), але «забруднює» URL.

Header-версіонування: «чисті» URL, відповідає REST-пуризму, але складніше для клієнтів, важко тестувати без інструментів, CDN не відрізняє версії без Vary-header.

На практиці URL-версіонування — домінантний вибір у індустрії (GitHub, Stripe, Twilio).

</details>

---

**[⬅️ Лекція 7: API Design](07_api_design.md)** | **[P08: Testing Practice ➡️](p08_testing_practice.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
