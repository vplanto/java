# Відповіді до екзаменаційного пулу (лише для викладача)

> **УВАГА:** цей файл не для студентів. Не публікуйте в відкритому доступі разом із матеріалами курсу.

Пул питань: [exam.md](exam.md)

---

## Тема 1: Engineering Mindset та SDLC

1. **Coder** фокусується на написанні коду «щоб працювало»; **Engineer** — на ризиках, підтримуваності, тестах, NFR, життєвому циклі та вартості змін.
2. Кожен рядок коду потребує супроводу, тестів, рев'ю; більше коду = більше боргу та ризику. Цінність — у рішенні проблеми, не в обсязі коду.
3. **AI Ownership** — студент/розробник відповідає за згенерований код так само, як за власний; має розуміти, тестувати й захищати рішення; «ШІ згенерував» не є виправданням.
4. Waterfall: жорсткі фази, пізнє тестування — ризик для швидких змін (стартапи). У медицині/космосі потрібна повна документація, валідація, traceability — waterfall або гібрид обов'язкові.
5. **Scrum** — фіксовані спринти, ролі, церемонії; **Kanban** — без спринтів, WIP-ліміти, безперервний потік. Scrum — продукт з релізами; Kanban — підтримка/ops, непередбачуваний потік задач.

## Тема 2: JVM

1. `javac` → `.class` (байт-код) → JVM (інтерпретація + JIT компіляція гарячих методів у машинний код) → WORA через абстракцію ОС.
2. **Stack** — локальні змінні, виклики методів, по одному на потік; **Heap** — об'єкти, спільний для потоків, GC. Stack зникає з потоком; об'єкти в heap живуть до GC.
3. **Циклічні посилання** (circular references) ламають reference counting. Рішення: **tracing GC** (mark-and-sweep, generational тощо).
4. Constant Pool зберігає літерали (рядки, числа), імена класів/методів; економить пам'ять через інтернування рядків, використовується при завантаженні класу.

## Тема 3: Requirements

1. User Story — цінність для користувача («як… хочу… щоб…»); ТЗ — технічна специфікація реалізації.
2. AC — умови приймання story; Given/When/Then робить їх перевірюваними та однозначними.
3. Вертикальний slice — повна функція через усі шари (UI/API/DB); горизонтальний — «тільки БД» без демонстрованої цінності.
4. **INVEST** (Independent, Negotiable, Valuable, Estimable, Small, Testable); Valuable відсікає «рефакторинг БД» без користувацької цінності.
5. BAU: логування, бекапи, моніторинг, безпека — замовник приймає як даність.

## Тема 4: NFRs

1. «Швидко» не вимірювано. Правильно: p95 latency < 200 ms при 1000 RPS, або час відповіді пошуку < 1 с.
2. Vertical — потужніший сервер; horizontal — більше вузлів. Без stateless/розділення стану горизонтальне масштабування неможливе (сесії на одному вузлі, локальні файли).
3. **SLA** — контракт з клієнтом; **SLO** — внутрішня ціль; availability виражається як % uptime у SLA/SLO.
4. **ADR** документує контекст, рішення, наслідки; «чому ні» зберігає знання для майбутніх команд.

## Тема 5: SOLID / Патерни

1. Інкапсуляція: контроль інваріантів, зміна реалізації без зламу клієнтів.
2. OCP: розширення через нові підкласи/обгортки (Decorator), не зміна існуючого коду.
3. DIP: залежність від абстракцій; DI — передача залежностей ззовні (constructor injection).
4. **Strategy**.
5. Глобальний стан, важко тестувати, приховані залежності, проблеми в distributed/microservices.

## Тема 6: Колекції / Generics

1. **List** (порядок, дублікати), **Set** (унікальність), **Queue** (FIFO/priority). Map окремо.
2. Map — пари key-value, інша абстракція, не колекція елементів у сенсі Collection.
3. ArrayList — зсув елементів у масиві O(n); LinkedList — вставка за посиланням O(1) при наявності вузла.
4. **LinkedHashSet** — унікальність + порядок вставки.
5. Java erasure — типи стираються в runtime; C++ templates — генерація коду для кожного типу.

## Тема 7: Concurrency

1. Race condition — результат залежить від порядку потоків; `count++` read-modify-write не атомарний.
2. `synchronized` — взаємовиключність (монітор); ціна — блокування, contention, зниження паралелізму.
3. Overhead створення потоку, unbounded threads, context switching, OOM.
4. `shutdown()` — graceful stop: не приймає нові задачі, дочікується виконання; без цього JVM може завершитись до виконання задач.
5. `Runnable` — `void run()`; `Callable` — повертає значення, кидає checked exceptions.

## Тема 8: Exceptions / Logging

1. **Error** — серйозні проблеми JVM (OutOfMemoryError); **Exception** — відновлювані ситуації (IOException).
2. Checked — компілятор вимагає handle/declare; Unchecked (RuntimeException) — помилки програмування.
3. Dev хоче DEBUG, prod — WARN/ERROR; рівні дозволяють фільтрувати без зміни коду.
4. Конкатенація завжди будує рядок; `{}` placeholders — lazy, якщо рівень вимкнено.
5. **MDC** — correlation id per request у багатопотоковому контексті для трасування логів.

## Тема 9: JDBC / DB

1. CAP: при partition — вибір C vs A; NoSQL часто AP або CP залежно від продукту.
2. SQL — схема, ACID, joins; NoSQL — document, key-value, column, graph (MongoDB, Redis, Cassandra).
3. **PreparedStatement** — параметризовані запити, захист від SQL injection.
4. Load driver → getConnection → createStatement/PreparedStatement → executeQuery/Update → process ResultSet → close resources.

## Тема 10: API Design

1. PUT — повна заміна ресурсу; PATCH — часткове оновлення; PUT для partial — семантично хибно.
2. 401 — не автентифікований; 403 — автентифікований, але без прав (напр. user vs admin).
3. Breaking: видалення поля, зміна типу; non-breaking: нове optional поле, новий endpoint.
4. Так, breaking для клієнтів що парсять timestamp; версіонування API, dual field, feature flag, deprecation period.
5. Single source of truth, паралельна розробка Frontend/Backend/QA, зменшення integration surprises.

## Тема 11: QA

1. Багато швидких unit внизу, менше integration, мінімум повільних E2E.
2. BVA — дефекти на межах діапазонів (0, max, max+1).
3. Flaky — інколи pass/fail; руйнує довіру до CI («зелений білд нічого не значить»).
4. AAA — підготовка, дія, перевірка; змішування ускладнює читання.
5. Quality Gate — пороги coverage, smells, vulnerabilities; maintainability як NFR.
6. Equivalence — класи еквівалентності; Decision Table — комбінації умов (складна бізнес-логіка).

## Тема 12: Docker

1. Image — незмінний шаблон (layers); Container — runtime instance образу.
2. Multi-stage — build у важкому образі, copy artifact у slim runtime; менший attack surface.
3. Docker кешує layers; зміна раннього layer інвалідує наступні; залежності копіювати перед кодом.
4. ENV в image — секрет у шарах; secrets/runtime env, Docker secrets, vault, не в Git.

## Тема 13: Distributed / Refactoring

1. Circuit Breaker — зупиняє каскадні виклики при збої downstream.
2. Tracing — end-to-end request id через сервіси; logging — локальні події без контексту ланцюжка.
3. Technical debt — відкладена якість; причини: дедлайни, незнання, copy-paste, відсутність тестів.
4. Refactoring — зміна структури без зміни поведінки; оптимізація — швидкість; bugfix — виправлення помилки.
5. Strangler Fig — поступова заміна legacy новим модулем через facade/proxy.

## Тема 14: Інструменти розробника (Git, Maven, DI)

1. Навіщо Git branch у командній розробці? Що робить `merge` vs `rebase` на високому рівні?
2. Яку роль відіграє `pom.xml` у Maven-проєкті? Назвіть фази `compile` та `test`.
3. Чому Constructor Injection кращий за field `@Autowired` з точки зору тестування та immutability?
4. Що таке `.gitignore` і чому не слід комітити `target/` та файли з секретами?
5. У чому різниця між `git clone` та `git fork` у контексті здачі домашніх завдань на GitHub?

### Ключові тези (Тема 14)

1. Branch — ізольована розробка; merge — злиття історій; rebase — перебазування комітів на іншу гілку (лінійна історія).
2. `pom.xml` — координати, залежності, plugins, lifecycle; compile — компіляція; test — unit tests.
3. Constructor injection — явні залежності, final fields, легко підставити mock у тесті; field injection — приховані залежності.
4. `.gitignore` виключає артефакти збірки та локальні секрети з репозиторію.
5. Clone — копія репо; fork — копія на GitHub для PR у чужий upstream (типовий workflow здачі).
