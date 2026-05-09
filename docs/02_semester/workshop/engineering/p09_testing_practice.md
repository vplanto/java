# Практикум P09: Testing на практиці. Пишемо тести, що ловлять баги

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 8: Test Cases & Coverage](../../08_test_cases.md), Spring Boot сервіс (P03–P05)

> **English version:** [English](en/p09_testing_practice.md)

---

## Мета заняття

Написати повноцінну тест-піраміду для `library-service`: Unit-тести для бізнес-логіки (`LoanFineCalculator`, `BookService`), та Controller-тест для HTTP-шару (`BookController`) через `@WebMvcTest`.

---

## Частина 1: Unit-тести бізнес-логіки (20 хв)

### Вправа 1.1: Equivalence Partitioning

Є сервіс розрахунку штрафу за прострочену книгу:

```java
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

Визначте класи еквівалентності і напишіть тести для кожного.

<details markdown="1">
<summary>Розв'язок</summary>

Класи еквівалентності:
- Здали вчасно (або раніше)
- Прострочили, але в межах grace period (1–3 дні)
- Прострочили понад grace period (4+ дні)
- Граничні значення: рівно 3 дні, рівно 4 дні

```java
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
        // Boundary: 4 дні — перший платний день
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

### Вправа 1.2: Мокування залежностей

Протестуйте `LoanService.returnBook()`. Використайте Mockito для ізоляції від реальної БД:

```java
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanFineCalculator fineCalculator;
    private final NotificationService notificationService;

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

<details markdown="1">
<summary>Тести з Mockito</summary>

```java
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock LoanFineCalculator fineCalculator;
    @Mock NotificationService notificationService;

    @InjectMocks LoanService loanService;

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
        Loan loan = new Loan(1L, 42L, LocalDate.of(2024, 3, 10), LoanStatus.ACTIVE);
        given(loanRepository.findById(1L)).willReturn(Optional.of(loan));
        given(fineCalculator.calculate(any(), any())).willReturn(new BigDecimal("7.50"));

        loanService.returnBook(1L, LocalDate.of(2024, 3, 17));

        verify(notificationService).sendFineNotification(eq(42L), eq(new BigDecimal("7.50")));
    }

    @Test
    void should_throw_when_loan_not_found() {
        given(loanRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnBook(999L, LocalDate.now()))
            .isInstanceOf(LoanNotFoundException.class);

        verifyNoInteractions(notificationService);
    }
}
```

</details>

---

## Частина 2: Unit-тест `BookService` (20 хв)

### Вправа 2.1: Тестування in-memory сервісу

`BookService` зберігає книги у `List<BookResponse>` та `AtomicLong` (P04). Тут немає БД — тому тест простий і швидкий: просто `new BookService(...)`, без Spring.

> [!NOTE]
> Ми не використовуємо `@DataJpaTest` — JPA у цьому проєкті відсутній. Тестуємо бізнес-логіку сервісу напряму.

```java
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
        assertThat(books).hasSize(2); // seed-дані з конструктора (P04)
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

        // Четверта — має впасти
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
> **Чому це краще ніж `@DataJpaTest`?**
> `@DataJpaTest` підіймає Spring-контекст і H2 — це 3–5 секунд на старт. Тест вище виконується за мілісекунди і не має зовнішніх залежностей. Саме такі тести складають основу **піраміди тестування**.

---

## Частина 3: Controller-тест (MockMvc) (20 хв)

### Вправа 3.1: @WebMvcTest

Протестуйте `BookController.getAllBooks()` та `BookController.getBook()` без підняття реального сервера:

```java
@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

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

```java
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean BookService bookService;

    @Test
    void getAllBooks_shouldReturnList() throws Exception {
        given(bookService.findAll())
            .willReturn(List.of(new BookResponse(1L, "Clean Code", "Robert C. Martin")));

        mockMvc.perform(get("/api/books")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void getBook_shouldReturn404_whenNotFound() throws Exception {
        given(bookService.findById(999L))
            .willThrow(new BookNotFoundException("Book with id 999 not found"));

        mockMvc.perform(get("/api/books/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book with id 999 not found"));
    }

    @Test
    void getBook_shouldReturn400_whenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/books/abc"))
            .andExpect(status().isBadRequest());
    }
}
```

`@WebMvcTest` підіймає тільки MVC-шар. BookService мокується через `@MockBean`.

</details>

---

## Частина 4: Аналіз coverage (10 хв)

### Вправа 4.1: Знайди прогалини

Подивіться на звіт Coverage для `LoanFineCalculator.calculate()`. Що означає 100% line coverage, але 80% branch coverage?

<details markdown="1">
<summary>Відповідь</summary>

100% line coverage: кожен рядок коду виконувався хоча б один раз.
80% branch coverage: 20% гілок (умови `if`/`else`) не були покриті.

Для `if (daysLate <= FREE_GRACE_DAYS)` потрібні два тести:
- один де умова `true` (daysLate ≤ 3)
- один де умова `false` (daysLate > 3)

Ви могли виконати обидва рядки, але тільки одну гілку — тоді лінії зелені, а branch — ні.

Branch coverage знаходить більше помилок, ніж line coverage. Mutation testing (PIT) — ще покращений підхід: перевіряє, чи зміна `<=` на `<` зламає тест.

</details>

---

## Контрольні питання

1. **Архітектурне питання:** Чому в `LoanServiceTest` ми використовуємо `@Mock` (Mockito), а в `BookControllerTest` — `@MockBean` (Spring)? Яка між ними різниця?

<details markdown="1">
<summary>Відповідь</summary>

`@Mock` — чистий Mockito мок, не залежить від Spring. Використовується разом з `@ExtendWith(MockitoExtension.class)` без підняття Spring Context. Швидкий.

`@MockBean` — реєструє мок у Spring Application Context. Потрібен коли тест підіймає Spring (як `@WebMvcTest`), щоб Spring міг впроваджувати мок у контролер через DI. Повільніший через ініціалізацію контексту.

Правило: якщо тестуєте без Spring — `@Mock`. Якщо Spring context потрібен — `@MockBean`.

</details>

2. **Flaky test:** Ваш тест `should_return_overdue_loans` іноді падає. Ви помітили, що він залежить від поточної дати (`LocalDate.now()`). Як виправити?

<details markdown="1">
<summary>Відповідь</summary>

Передавати `Clock` як залежність замість виклику `LocalDate.now()` напряму:

```java
// Сервіс:
public List<Loan> getOverdueLoans(Clock clock) {
    return loanRepository.findOverdueLoans(LocalDate.now(clock));
}

// Тест:
LocalDate fakeToday = LocalDate.of(2024, 6, 1);
Clock fixedClock = Clock.fixed(fakeToday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
List<Loan> result = loanService.getOverdueLoans(fixedClock);
```

Так тест детермінований і не залежить від системного часу.

</details>

3. **Performance:** `@SpringBootTest` піднімає весь контекст. Чому це проблема, якщо таких тестів 50?

<details markdown="1">
<summary>Відповідь</summary>

`@SpringBootTest` займає 3–10 секунд на ініціалізацію. 50 таких тестів = 4–8 хвилин тільки на старт контексту.

Рішення: використовувати мінімально необхідний slice-тест:
- `@WebMvcTest` — тільки MVC-шар
- `@MockBean` — мокуємо те, що не тестуємо
- Для in-memory сервісів — просто `new Service(...)` без жодних анотацій

`@SpringBootTest` залишаємо лише для end-to-end інтеграційних тестів, яких повинно бути мало (верхівка піраміди).

</details>

---

**[⬅️ Лекція 8: Test Cases](../../08_test_cases.md)** | **[P06: Docker на практиці ➡️](p06_docker_practice.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
