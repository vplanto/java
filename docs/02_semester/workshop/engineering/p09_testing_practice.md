# Практикум P09: Testing на практиці. Пишемо тести, що ловлять баги

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 8: Test Cases & Coverage](../../08_test_cases.md), Spring Boot сервіс (P03–P05), згенерований та налаштований у [P08: API Design на практиці](p08_api_practice.md)

> **English version:** [English](en/p09_testing_practice.md)

---

## Мета заняття

Побудувати повноцінну тест-піраміду для нашого `library-service`: від швидких Unit-тестів бізнес-логіки (`LoanFineCalculator`, `BookService`) до Controller-тестів для перевірки HTTP-контрактів (`BookController`) за допомогою `@WebMvcTest` та `MockMvc`. Ми навчимося писати тести, які дійсно запобігають багам та фіксують інженерні контракти.

---

## Частина 1: Unit-тести бізнес-логіки (20 хв)

### Бізнес-сценарій: Лояльна бібліотека та штрафи
Наш бібліотечний сервіс видає книги читачам на певний термін. Ми хочемо стимулювати читачів повертати книги вчасно, тому ввели систему штрафів. Проте наша бібліотека є дружньою, тому ми надаємо **grace period** (пільговий період) у 3 дні. Якщо читач затримав книгу на 1, 2 чи 3 дні — штраф не нараховується. Починаючи з 4-го дня затримки, нараховується фіксований штраф у розмірі 2.50 грн за кожен день прострочення понад grace period (тобто за 4-й день затримки штраф складе 2.50 грн, за 5-й день — 5.00 грн і так далі).

За розрахунок штрафу в нашому додатку відповідає клас `LoanFineCalculator`.

**Файл: src/main/java/ua/edu/libraryservice/service/LoanFineCalculator.java**
```java
package ua.edu.libraryservice.service;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class LoanFineCalculator {
    private static final BigDecimal DAILY_FINE = new BigDecimal("2.50");
    private static final int FREE_GRACE_DAYS = 3;

    public BigDecimal calculate(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (daysLate <= FREE_GRACE_DAYS) return BigDecimal.ZERO;
        return DAILY_FINE.multiply(BigDecimal.valueOf(daysLate - FREE_GRACE_DAYS));
    }
}
```

### Вправа 1.1: Equivalence Partitioning для LoanFineCalculator
#### Інженерний виклик
Якщо ми будемо писати окремий тест для кожного можливого варіанту кількості днів запізнення (4 дні, 5 днів, 6 днів, 10 днів...), ми напишемо забагато дублюючого коду.
Замість цього застосуємо техніку **Equivalence Partitioning** (розбиття на класи еквівалентності) та **Boundary Value Analysis** (аналіз граничних значень). Ми об'єднаємо всі можливі вхідні дані у класи, які обробляються однаково, та перевіримо поведінку коду на межах цих класів.

Визначте класи еквівалентності і напишіть тести для кожного.

<details markdown="1">
<summary>Розв'язок</summary>

Класи еквівалентності:
- Здали вчасно або раніше (`daysLate <= 0`) — штраф 0.00
- Прострочили, але в межах пільгового періоду (`1 <= daysLate <= 3`) — штраф 0.00
- Прострочили понад пільговий період (`daysLate >= 4`) — нараховується штраф

Граничні значення (Boundaries):
- Рівно 3 дні (останній безкоштовний день).
- Рівно 4 дні (перший платний день).

**Файл: src/test/java/ua/edu/libraryservice/service/LoanFineCalculatorTest.java**
```java
package ua.edu.libraryservice.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanFineCalculatorTest {

    private final LoanFineCalculator calculator = new LoanFineCalculator();
    private static final LocalDate DUE_DATE = LocalDate.of(2024, 3, 10);

    @Test
    void should_return_zero_when_returned_on_time() {
        BigDecimal fine = calculator.calculate(DUE_DATE, DUE_DATE);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_return_zero_when_returned_early() {
        BigDecimal fine = calculator.calculate(DUE_DATE, DUE_DATE.minusDays(2));
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_return_zero_within_grace_period() {
        // Boundary: рівно 3 дні — ще в grace
        BigDecimal fine = calculator.calculate(DUE_DATE, DUE_DATE.plusDays(3));
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_charge_fine_for_first_day_after_grace() {
        // Boundary: 4 дні — перший платний день (штраф за 1 день)
        BigDecimal fine = calculator.calculate(DUE_DATE, DUE_DATE.plusDays(4));
        assertThat(fine).isEqualByComparingTo("2.50");
    }

    @Test
    void should_charge_fine_for_multiple_days() {
        // 10 днів прострочення → (10-3) * 2.50 = 17.50
        BigDecimal fine = calculator.calculate(DUE_DATE, DUE_DATE.plusDays(10));
        assertThat(fine).isEqualByComparingTo("17.50");
    }
}
```

</details>

### Вправа 1.2: Мокування залежностей (Mocking) для LoanService
#### Бізнес-сценарій: Повернення книги та сповіщення
Уявіть бізнес-вимогу: коли читач повертає книгу до бібліотеки, система повинна знайти запис позики в базі даних, розрахувати штраф за протермінування, зберегти зміни та, якщо штраф нараховано, надіслати читачеві сповіщення.

**Архітектурне питання:** Де саме ми маємо розмістити цю координаційну логіку — в DTO, контролері, сервісі чи, можливо, в окремому інфраструктурному біні?

<details markdown="1">
<summary>Відповідь</summary>

Логіку оркестрації повернення книги ми розміщуємо у **сервісі** (`LoanService`):
- **DTO** (Data Transfer Object) — це просто тупий контейнер для даних. Він не повинен містити жодної бізнес-логіки.
- **Контролер** — це шар представлення (API). Він має бути максимально "тонким" і займатися лише обробкою HTTP-запитів, статус-кодів та валідацією контракту.
- **Сервіс** (Service Layer) — ідеальне місце для координації бізнес-процесу. Він оркеструє роботу репозиторію, калькулятора та служби сповіщень.
- **Окремі біни/компоненти** (як `LoanFineCalculator`) — використовуються для винесення складної ізольованої логіки (наприклад, математичних розрахунків), яку легко протестувати юніт-тестами без моків.

</details>

Отже, наш сервіс `LoanService` має виконувати бізнес-операцію повернення за такими кроками:
1. Знайти активну позику (`Loan`) в базі даних. Якщо її немає — кинути помилку `LoanNotFoundException`.
2. Розрахувати суму штрафу за допомогою `LoanFineCalculator`.
3. Змінити статус позики на `RETURNED`, записати суму штрафу і зберегти зміни в базу даних через `LoanRepository`.
4. Якщо нараховано штраф (більше 0), надіслати сповіщення про це читачеві через зовнішній `NotificationService`.

<details markdown="1">
<summary>Код LoanRepository</summary>

**Файл: src/main/java/ua/edu/libraryservice/repository/LoanRepository.java**
```java
package ua.edu.libraryservice.repository;

import ua.edu.libraryservice.model.Loan;
import java.util.Optional;

public interface LoanRepository {
    Optional<Loan> findById(Long id);
    Loan save(Loan loan);
}
```
</details>

**Файл: src/main/java/ua/edu/libraryservice/service/LoanService.java**
```java
package ua.edu.libraryservice.service;

import org.springframework.stereotype.Service;
import ua.edu.libraryservice.dto.LoanReturnResult;
import ua.edu.libraryservice.dto.LoanStatus;
import ua.edu.libraryservice.exception.LoanNotFoundException;
import ua.edu.libraryservice.model.Loan;
import ua.edu.libraryservice.repository.LoanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanFineCalculator fineCalculator;
    private final NotificationService notificationService;

    public LoanService(LoanRepository loanRepository,
                       LoanFineCalculator fineCalculator,
                       NotificationService notificationService) {
        this.loanRepository = loanRepository;
        this.fineCalculator = fineCalculator;
        this.notificationService = notificationService;
    }

    public LoanReturnResult returnBook(Long loanId, LocalDate returnDate) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        BigDecimal fine = fineCalculator.calculate(loan.getDueDate(), returnDate);
        loan.setStatus(LoanStatus.RETURNED);
        loan.setFineAmount(fine);
        loanRepository.save(loan);

        if (fine.compareTo(BigDecimal.ZERO) > 0) {
            notificationService.sendFineNotification(loan.getReaderId(), fine);
        }

        return new LoanReturnResult(loanId, fine);
    }
}
```

#### Інженерний виклик
Як протестувати логіку `LoanService` без підключення до реальної бази даних (через `LoanRepository`) та без відправки реальних сповіщень (через `NotificationService`)?
Якщо ми спробуємо запустити тест із справжніми інфраструктурними компонентами, вони будуть занадто повільними, нестабільними та вимагатимуть складної конфігурації.
Тут нам на допомогу приходить **Mockito** — фреймворк для створення заглушок (Mocks). Ми замінимо залежності нашого сервісу на керовані моки, поведінку яких ми зможемо програмувати в тестах за допомогою `given()` та перевіряти за допомогою `verify()`.

Протестуйте `LoanService.returnBook()`. Використайте Mockito для ізоляції від інфраструктури.

<details markdown="1">
<summary>Розв'язок</summary>

**Файл: src/test/java/ua/edu/libraryservice/service/LoanServiceTest.java**
```java
package ua.edu.libraryservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.libraryservice.dto.LoanReturnResult;
import ua.edu.libraryservice.dto.LoanStatus;
import ua.edu.libraryservice.exception.LoanNotFoundException;
import ua.edu.libraryservice.model.Loan;
import ua.edu.libraryservice.repository.LoanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanFineCalculator fineCalculator;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanService loanService;

    @Test
    void should_return_book_without_fine_and_not_send_notification() {
        // Arrange
        Loan loan = new Loan(1L, 42L, LocalDate.of(2024, 3, 10), LoanStatus.ACTIVE);
        given(loanRepository.findById(1L)).willReturn(Optional.of(loan));
        given(fineCalculator.calculate(any(), any())).willReturn(BigDecimal.ZERO);

        // Act
        LoanReturnResult result = loanService.returnBook(1L, LocalDate.of(2024, 3, 9));

        // Assert
        assertThat(result.fineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(loanRepository).save(argThat(l -> l.getStatus() == LoanStatus.RETURNED));
        verifyNoInteractions(notificationService);  // fine = 0 → повідомлення немає
    }

    @Test
    void should_send_fine_notification_when_overdue() {
        // Arrange
        Loan loan = new Loan(1L, 42L, LocalDate.of(2024, 3, 10), LoanStatus.ACTIVE);
        given(loanRepository.findById(1L)).willReturn(Optional.of(loan));
        given(fineCalculator.calculate(any(), any())).willReturn(new BigDecimal("7.50"));

        // Act
        loanService.returnBook(1L, LocalDate.of(2024, 3, 17));

        // Assert
        verify(notificationService).sendFineNotification(eq(42L), eq(new BigDecimal("7.50")));
    }

    @Test
    void should_throw_when_loan_not_found() {
        // Arrange
        given(loanRepository.findById(999L)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> loanService.returnBook(999L, LocalDate.now()))
            .isInstanceOf(LoanNotFoundException.class);

        verifyNoInteractions(notificationService);
    }
}
```

</details>

---

## Частина 2: Unit-тест `BookService` (20 хв)

### Бізнес-сценарій: Керування каталогом книг
У нашому застосунку `BookService` відповідає за збереження та валідацію книг. Оскільки ми поки що зберігаємо книги in-memory (просто у списку `List<BookResponse>`), у нас немає потреби підіймати базу даних. Ми маємо перевірити такі правила:
1. При ініціалізації додаються початкові (seed) книги.
2. Не можна додати книгу з порожньою назвою.
3. Не можна перевищити максимальний ліміт кількості книг у бібліотеці (параметр, що зчитується з конфігурації у P05).
4. Якщо книга з певним ID не знайдена, кидається `BookNotFoundException`.

### Інженерний виклик
Багато розробників-початківців автоматично додають `@SpringBootTest` до будь-якого тесту. Але `@SpringBootTest` підіймає весь контекст Spring, шукає всі біни та конфігурації. Це займає від 3 до 10 секунд на старт. Якщо таких тестів буде 100, збірка проєкту в CI/CD триватиме хвилини.
Оскільки наш `BookService` не має зовнішніх залежностей від інфраструктури, ми можемо протестувати його як звичайний Java-клас: створити його вручну через `new BookService("Test Library", 3)` у секції `@BeforeEach`. Такі тести виконуються за частки мілісекунди.

> [!NOTE]
> Ми не використовуємо `@DataJpaTest` — JPA у цьому проєкті відсутній. Тестуємо бізнес-логіку сервісу напряму як чистий Java-код.

**Файл: src/test/java/ua/edu/libraryservice/service/BookServiceTest.java**
```java
package ua.edu.libraryservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.edu.libraryservice.dto.BookResponse;
import ua.edu.libraryservice.exception.BookNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookServiceTest {

    private BookService bookService;

    @BeforeEach
    void setUp() {
        // Створюємо сервіс вручну з тестовими параметрами конфігурації
        bookService = new BookService("Test Library", 3);
    }

    @Test
    void findAll_shouldReturnSeedBooks() {
        List<BookResponse> books = bookService.findAll();
        assertThat(books).hasSize(2); // seed-дані з конструктора (P04/P05)
    }

    @Test
    void addBook_shouldReturnBookWithGeneratedId() {
        BookResponse book = bookService.addBook("Clean Code", "Robert C. Martin");
        assertThat(book.id()).isNotNull();
        assertThat(book.title()).isEqualTo("Clean Code");
    }

    @Test
    void addBook_whenLimitReached_shouldThrowIllegalArgumentException() {
        // Ліміт = 3, seed = 2 → одна книга ще влізе
        bookService.addBook("Extra Book", "Author");

        // Четверта книга — має викликати помилку ліміту
        assertThatThrownBy(() -> bookService.addBook("One Too Many", "Author"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("заповнена");
    }

    @Test
    void addBook_whenTitleIsBlank_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> bookService.addBook("", "Author"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title");
    }

    @Test
    void findById_whenNotFound_shouldThrowBookNotFoundException() {
        assertThatThrownBy(() -> bookService.findById(999L))
            .isInstanceOf(BookNotFoundException.class);
    }
}
```

> [!TIP]
> **Чому це краще ніж `@DataJpaTest` чи `@SpringBootTest`?**
> `@SpringBootTest` підіймає Spring-контекст — це 3–5 секунд на старт. Тест вище виконується за мілісекунди і не має зовнішніх залежностей. Саме такі "чисті" тести складають основу **піраміди тестування** (Unit-рівень).

---

## Частина 3: Controller-тест (MockMvc) (20 хв)

### Бізнес-сценарій: Стабільність HTTP контракту
У P08 мы спроектували API-контракт для нашого сервісу. Ми гарантували клієнтам:
1. `GET /api/books` повертає список книг із статусом `200 OK`.
2. `GET /api/books/{id}` повертає книгу, якщо вона існує. Якщо ні — повертає `404 Not Found` з структурованим об'єктом помилки `ApiError`.
3. Запит із неправильним типом параметра (наприклад, літери замість ID книги) повертає `400 Bad Request`.

### Інженерний виклик
Як протестувати ці правила HTTP-взаємодії, не запускаючи при цьому реальний веб-сервер (Tomcat) і не виконуючи справжніх HTTP-запитів по мережі?
Spring Boot надає інструмент `@WebMvcTest` разом із `MockMvc`. Вони дозволяють протестувати лише веб-шар (Spring MVC) в ізоляції. При цьому:
- Контекст Spring завантажує лише контролери, фільтри та `GlobalExceptionHandler`.
- Всі сервіси (наприклад, `BookService`) заміняються на моки за допомогою `@MockBean`.
- `MockMvc` дозволяє симулювати HTTP-запити в пам'яті та перевіряти відповіді за допомогою виразного API (флюент-методів `status()`, `jsonPath()`).

Протестуйте `BookController`, використовуючи `@WebMvcTest`.

Згадаємо структуру нашого контролера:

**Файл: src/main/java/ua/edu/libraryservice/controller/BookController.java**
```java
package ua.edu.libraryservice.controller;

import org.springframework.web.bind.annotation.*;
import ua.edu.libraryservice.dto.BookResponse;
import ua.edu.libraryservice.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable Long id) {
        return bookService.findById(id);
    }
}
```

<details markdown="1">
<summary>Тест з MockMvc</summary>

**Файл: src/test/java/ua/edu/libraryservice/controller/BookControllerTest.java**
```java
package ua.edu.libraryservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.libraryservice.dto.BookResponse;
import ua.edu.libraryservice.exception.BookNotFoundException;
import ua.edu.libraryservice.service.BookService;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void getAllBooks_shouldReturnList() throws Exception {
        // Given
        given(bookService.findAll())
            .willReturn(List.of(new BookResponse(1L, "Clean Code", "Robert C. Martin")));

        // When & Then
        mockMvc.perform(get("/api/books")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void getBook_shouldReturn404_whenNotFound() throws Exception {
        // Given
        given(bookService.findById(999L))
            .willThrow(new BookNotFoundException("Book with id 999 not found"));

        // When & Then
        mockMvc.perform(get("/api/books/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book with id 999 not found"));
    }

    @Test
    void getBook_shouldReturn400_whenIdIsNotANumber() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/books/abc"))
            .andExpect(status().isBadRequest());
    }
}
```

`@WebMvcTest` підіймає тільки MVC-шар. `BookService` мокується через `@MockBean`.

</details>

---

## Частина 4: Аналіз coverage (10 хв)

### Бізнес-сценарій: Ілюзія безпеки
Команда розробників написала тести для `LoanFineCalculator.calculate()`. Менеджер проєкту перевірив звіт у SonarQube і побачив гарне число: **100% Line Coverage** (покриття рядків коду). Всі задоволені, реліз коду пішов на production.
Проте через тиждень бібліотека виявила фінансові втрати: деяким читачам неправильно розраховувався штраф, і вони не платили нічого за прострочені книги. Як таке могло статися, якщо покриття було 100%?

### Вправа 4.1: Знайди прогалини
#### Інженерний виклик: Line Coverage vs Branch Coverage
Line Coverage показує лише те, що кожен рядок коду хоча б один раз виконувався під час тестів. Але він ігнорує логічні розгалуження.

Подивімося знову на рядок:
```java
if (daysLate <= FREE_GRACE_DAYS) return BigDecimal.ZERO;
```

Якщо мы написали лише два тести:
1. Повернуто вчасно (`daysLate = 0` → умова `true`, повертає `ZERO`).
2. Повернуто з великою затримкою (`daysLate = 10` → умова `false`, повертає штраф).

Ми виконали обидва рядки, отже Line Coverage = 100%.
Але що означає 100% line coverage, але тільки 80% branch coverage для всього методу?

<details markdown="1">
<summary>Аналіз та відповідь</summary>

100% line coverage: кожен рядок коду виконувався хоча б один раз.
80% branch coverage: 20% гілок (умови `if`/`else`) не були покриті.

Для повноцінного тестування `if (daysLate <= FREE_GRACE_DAYS)` нам критично протестувати граничні значення:
- `daysLate = 3` (умова `true`, повертає `ZERO`) — межа пільгового періоду.
- `daysLate = 4` (умова `false`, нараховує штраф за 1 день) — перша точка виходу за межу.

Якщо розробник помилився і написав `if (daysLate < FREE_GRACE_DAYS)` (використав `<` замість `<=`), то при 3 днях запізнення клієнт вже отримав би штраф. Звичайний тест на 0 днів та 10 днів не виявив би цього багу! Тільки тест на граничне значення `daysLate = 3` помітить помилку.

Branch coverage знаходить набагато більше помилок, ніж line coverage, оскільки вимагає покрити всі логічні шляхи.

Ще потужнішим інструментом є **Mutation Testing** (мутаційне тестування, наприклад фреймворк PIT). Він автоматично модифікує ваш байт-код (змінює `<=` на `<`, `+` на `-`, `true` на `false`) і запускає тести. Якщо після зміни коду тести продовжують успішно проходити (зелені) — отже, ваші тести слабкі («мутант вижив»). Якщо хоча б один тест впав — «мутант убитий», ваші тести якісні.

</details>

---

## Контрольні питання

1. **Архітектурний вибір моків:** Чому в `LoanServiceTest` ми використовуємо `@Mock` (Mockito), а в `BookControllerTest` — `@MockBean` (Spring)? Яка між ними принципова різниця в контексті швидкості виконання тестів?

<details markdown="1">
<summary>Відповідь</summary>

- `@Mock` — це чистий Mockito-мок, він не залежить від Spring. Використовується разом з `@ExtendWith(MockitoExtension.class)`. Такі тести є класичними unit-тестами, вони не підіймають контекст програми і виконуються за частки мілісекунди.
- `@MockBean` — це Spring-специфічна анотація. Вона створює Mockito-мок і реєструє його в Spring Application Context. Це необхідно при інтеграційному або slice-тестуванні (як `@WebMvcTest`), щоб Spring міг впровадити цей мок у залежності контролера через Dependency Injection (DI). Такі тести повільніші, бо потребують ініціалізації контексту (3–5 секунд).

**Правило:** якщо тестуєте звичайну логіку без використання фреймворку — обирайте швидкий `@Mock`. Якщо тестуєте інтеграцію з веб-шаром чи базою — використовуйте `@MockBean`.

</details>

2. **Боротьба з Flaky-тестами:** Ваш інтеграційний тест успішно проходить вдень, але раптово падає на нічній CI/CD збірці о 00:05. Ви виявили, що бізнес-логіка залежить від поточної дати (`LocalDate.now()`). Як зробити цей тест детермінованим і надійним?

<details markdown="1">
<summary>Відповідь</summary>

Замість виклику статичного `LocalDate.now()` безпосередньо в бізнес-коді, потрібно використовувати системний годинник `Clock` як залежність, яку можна заінжектувати:

**У бізнес-сервісі:**
```java
public class LoanService {
    private final Clock clock; // інжектується через конструктор

    public List<Loan> getOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        return loanRepository.findOverdueLoans(today);
    }
}
```

**У тестовому класі:**
```java
LocalDate fakeToday = LocalDate.of(2024, 6, 1);
Clock fixedClock = Clock.fixed(fakeToday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
// передаємо фіксований годинник у сервіс:
LoanService loanService = new LoanService(loanRepository, fineCalculator, notificationService, fixedClock);
```

Тепер годинник завжди повертає одну й ту саму дату, і тест є абсолютно стабільним (детермінованим) незалежно від часу запуску.

</details>

3. **Продуктивність збірки:** Розробник створив 50 тестів, і кожен з них помічений анотацією `@SpringBootTest`. Чому лід-інженер змусить його переписати ці тести, і яка альтернатива?

<details markdown="1">
<summary>Відповідь</summary>

Кожен `@SpringBootTest` намагається ініціалізувати весь контекст програми. Навіть із оптимізацією кешування контексту Spring, 50 таких тестів суттєво уповільнять збірку проєкту в CI/CD (до кількох хвилин).
Альтернатива — дотримуватися **піраміди тестування**:
- Основну частину тестів (80%) писати як швидкі **Unit-тести** (без Spring, звичайний `new Service(...)` та `@Mock`).
- Для веб-рівня використовувати легкі **Slice-тести** (`@WebMvcTest` + `MockMvc`), які завантажують лише мінімум веб-компонентів.
- `@SpringBootTest` залишати лише для декількох критичних наскрізних (End-to-End) інтеграційних тестів, які перевіряють роботу системи в цілому.

</details>

**[⬅️ P08: API Design на практиці](p08_api_practice.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
