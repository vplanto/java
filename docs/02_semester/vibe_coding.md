# Методологія Vibe Coding: Як ми навчаємо у 2025 році

**Ролі:** Студент (Product Owner & AI Operator) → Ментор (System Architect)

> **English version:** [English](en/vibe_coding.md)

---

## 1. Філософія

У 2025 році синтаксис — це commodity. GPT-4o напише `for`-цикл краще за більшість людей за секунду.

**Що залишилось людським завданням:**
- Розуміти *навіщо* система існує (Business Need → Solution)
- Проєктувати *як* вона не впаде під навантаженням (NFRs)
- Відповідати за рішення, яке прийняв AI (AI Ownership)

> **Правило курсу:** Ви можете не написати жодного рядка коду руками. Але ви зобов'язані розуміти і пояснити кожен рядок, який згенерував AI — *на рівні, достатньому для Code Audit*.

Цей підхід не замінює теорію — він вимагає її. Не можна захистити архітектурне рішення, не знаючи CAP-теореми. Не можна пояснити AI-код, не розуміючи, що таке Technical Debt і Code Smell.

---

## 2. Workflow: 4 етапи проєкту

### Етап 1 — The Pitch (студент)

Ви приходите з **ідеєю**, а не з ТЗ.

> *«Хочу застосунок для трекінгу поливу квітів, схожий на Тамагочі»*

**Артефакт:** One-pager — опис проблеми, яку вирішуємо, і хто буде користуватись.

Зв'язок з курсом: [Лекція 3 — Definition of Requirements](03_requirements.md), [Лекція 4 — NFRs](04_nfr.md).

---

### Етап 2 — Architectural Review (ментор + студент)

Перетворюємо «вайб» на інженерні вимоги. **Не обговорюємо кольори кнопок.** Обговорюємо дані та процеси.

| Питання | Чому це важливо |
| :--- | :--- |
| Які сутності? (`Plant`, `Schedule`, `WateringEvent`) | Дизайн бази даних |
| Де зберігаємо фото? S3 чи Base64 в БД? | NFR: Storage cost, latency |
| Який API контракт? GET/POST endpoints? | [Лекція 7 — API Design](07_api_design.md) |
| Скільки користувачів одночасно? | Scalability NFR |
| Що буде при падінні зовнішнього сервісу? | [Лекція 10 — Circuit Breaker](10_distributed_systems.md) |

**Quality Gate:** ментор підписує архітектуру до початку кодування.

---

### Етап 3 — AI Implementation (студент + AI)

Ви йдете в поле з AI. Ваша задача — **змусити AI дотримуватись погодженої архітектури**.

**AI-інструменти для Java/Spring Boot:**

| Інструмент | Для чого |
| :--- | :--- |
| **GitHub Copilot** / **Cursor** | Генерація коду прямо в IntelliJ IDEA |
| **Claude / ChatGPT** | Архітектурні рішення, рев'ю коду, debug |
| **Spring Initializr** | Скаффолдинг проєкту |
| **Render.com** | Деплой (CD) — без DevOps-знань |

**Правило промпту:** Завжди давайте AI контекст.

```
❌ Погано: "напиши REST endpoint для замовлень"

✅ Добре: "напиши Spring Boot @RestController для Order.
   Використовуй Constructor Injection (не @Autowired на полі).
   Повертай DTO, не Entity. Обробляй 404 через @ControllerAdvice.
   Стиль: без Lombok, Java records для DTO."
```

**Quality Gate:** MVP запускається локально (`docker compose up`) і проходить базові тести ([Практикум P08](p08_testing_practice.md)).

---

### Етап 4 — The Defense (ментор vs студент)

Найжорсткіший етап. **Code Audit** — ментор перевіряє, чи студент розуміє свій код.

**Типові питання за темами курсу:**

| Тема | Питання на Defense |
| :--- | :--- |
| [Л3 Requirements](03_requirements.md) | «Хто ваші Stakeholders? Що є Business Need?» |
| [Л4–5 NFR](04_nfr.md) | «Що станеться при 1000 одночасних юзерів? Де bottleneck?» |
| [Л7 API](07_api_design.md) | «Чому POST, а не PUT? Ваш endpoint ідемпотентний?» |
| [Л9 Docker](09_docker.md) | «Чому multi-stage build? Які шари в вашому Image?» |
| [Л10 Distributed](10_distributed_systems.md) | «Що буде, якщо зовнішнє API поверне 500? Є Circuit Breaker?» |
| [Л11 System Design](11_system_design.md) | «Як масштабуватимете при 10x трафіку?» |
| [Л12 Refactoring](12_refactoring.md) | «Де Technical Debt у вашому коді? Покажіть Code Smell.» |

**Результат:** PR приймається або відхиляється з коментарем.

---

## 3. Правила

### Дозволено ✅
- Будь-які AI-моделі
- Копіювати з документації та Stack Overflow
- Починати з готових бойлерплейтів (Spring Initializr)

### Red Flags ❌

| Порушення | Чому проблема | Де про це в курсі |
| :--- | :--- | :--- |
| **Magic Code** — код, призначення якого не можете пояснити | Єдина людина, що «знає» — AI | [Л12 Refactoring](12_refactoring.md) |
| **Hardcode Credentials** — паролі в коді | AI це робить автоматично | [Л9 Docker](09_docker.md) — `.env` |
| **God Objects** — клас на 1000 рядків | Code Smell: Large Class | [Л12 Refactoring](12_refactoring.md) |
| **Blind Trust** — вірити AI-коментарям | Вони часто описують не те, що робить код | [Л12](12_refactoring.md) |
| **No Error Handling** — `try { } catch (Exception e) {}` | API повертає 500 без причини | [Л7 API Design](07_api_design.md), [P05](p05_spring_production_ready.md) |

---

## 4. Критерії оцінювання

Оцінюємо **якість інженерних рішень**, не кількість рядків коду.

| Критерій | Що перевіряємо | Definition of Done |
| :--- | :--- | :--- |
| **Product Vision** | Чи вирішує додаток заявлену проблему? | Задокументований BACCM |
| **System Design** | Архітектура, відсутність спагеті-коду | Чиста шарова структура (Controller → Service → Repository) |
| **NFRs** | Чи враховані нефункціональні вимоги? | Є хоча б Scalability и Security NFR |
| **AI Ownership** | Впевнено захищає кожне рішення | Проходить Code Audit без «AI так написав» |
| **Deployment** | Продукт доступний в інтернеті | URL на Render.com або аналог |
| **Code Quality** | SonarLint без критичних Issues | 0 Critical, ≤5 Major |

---

## 5. Приклад взаємодії

> **Ментор:** «У вашому `ChatController` є `Thread.sleep(1000)`. Навіщо?»

> **❌ Погано:** «AI так написав, мабуть щоб не навантажувати сервер.» → **PR відхилено.**

> **✅ Добре:** «Це милиця для імітації затримки мережі під час локального тестування. У production треба прибрати і замінити асинхронною чергою (Kafka або RabbitMQ), щоб не блокувати thread-pool.» → **PR прийнятий.**

---

**[⬅️ Повернутися до головного меню курсу](index.md)**