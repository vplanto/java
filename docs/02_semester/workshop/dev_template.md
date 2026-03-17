# Dev: Технічна деталізація User Stories

**Проєкт:** VARTA (Distributed Resilience Orchestrator)
**Сквад:** Alpha / Beta / Gamma *(вказати свій)*
**GitHub Project:** [VARTA Board](https://github.com/users/vplanto/projects/1)
**Зв'язок:** [Product Backlog](product_backlog.md) | [План воркшопу](plan.md) | [SME](sme_template.md) | [UX](ux_template.md)

---

## Ціль ролі Dev

Ви — **розробник**. Ваше завдання — перетворити User Stories на **Technical Tasks** (конкретні технічні задачі), які можна оцінити в Story Points та взяти в спринт.

Кожен Technical Task — це **окремий тікет** у [GitHub Project](https://github.com/users/vplanto/projects/1). Тікет = ваш дизайн-артефакт.

> 📝 **Naming Convention:** Див. [Конвенція оформлення тікетів](plan.md#-конвенція-оформлення-тікетів-github-project). Заголовок англійською, тіло українською.

---

## Робочий процес

### Крок 1: Вивчіть вхідні артефакти

Перед створенням тасок перегляньте:
- **Product Backlog** → [User Stories](product_backlog.md) вашого Epic.
- **SME тікети** → `DR-*` (доменні правила) та `EC-*` (edge cases) — бізнес-обмеження, які впливають на архітектуру.
- **UX тікети** → `UF-*` (user flows) та `WF-*` (wireframes) — яку поведінку має реалізувати код.

### Крок 2: Декомпозуйте Stories на Technical Tasks

Кожну User Story розбийте на **2–5 технічних задач** за шарами:
- **API** — ендпоінти, контракти, валідація.
- **Domain / Service** — бізнес-логіка, алгоритми, CRDT.
- **Data** — моделі, сховище, міграції.
- **Integration** — зовнішні системи, Mesh, IoT.
- **UI** — відображення (якщо є фронтенд).

### Крок 3: Створіть тікети у GitHub Project

---

## Тип тікету

### 🏷 `TT` — Technical Task

Конкретна реалізаційна задача з чітким Definition of Done.

**Формат заголовку:** `TT-## EP-XX US-YY | Short English description`

**Тіло тікету (українською):**
```
**Тип:** Technical Task
**Сквад:** Alpha / Beta / Gamma
**Пов'язані Stories:** US-XX
**Labels:** tech-task, squad:xxx, EP-XX
**Шар:** API / Domain / Data / Integration / UI

**Опис:**
<Що конкретно потрібно зробити>

**Acceptance Criteria (технічні):**
- [ ] <Критерій 1>
- [ ] <Критерій 2>

**Залежності:** #<номер пов'язаного тікету>
```

<details>
<summary>📋 Приклад: Декомпозиція US-06 (Transfer Quota)</summary>

**User Story:** *"Як мешканець, хочу передати частину своєї квоти іншому учаснику."*

**Доменні правила від SME:**
- `DR-01`: Trust ≥ 2
- `DR-04`: Макс 50% від залишку

**User Flow від UX:**
- `UF-01`: 7 кроків від відкриття додатку до QR-підтвердження

**Технічні таски:**

---

**Заголовок:** `TT-01 EP-02 US-06 | Transfer Quota API endpoint`

```
**Тип:** Technical Task
**Сквад:** Beta
**Пов'язані Stories:** US-06
**Labels:** tech-task, squad:beta, EP-02
**Шар:** API

**Опис:**
Створити REST/gRPC ендпоінт для передачі квоти між вузлами.
Вхід: sender_id, receiver_id, resource_type, amount.
Вихід: transaction_id, status, new_balance.

**Acceptance Criteria (технічні):**
- [ ] Валідація Trust Level ≥ 2 (DR-01)
- [ ] Валідація amount ≤ 50% від balance (DR-04)
- [ ] Повертає 403 якщо Trust < 2
- [ ] Повертає 400 якщо amount > 50%
- [ ] Логує транзакцію для audit trail (US-22)

**Залежності:** #DR-01, #DR-04, #UF-01
```

---

**Заголовок:** `TT-02 EP-02 US-06 | CRDT merge for quota transfer`

```
**Тип:** Technical Task
**Сквад:** Beta
**Пов'язані Stories:** US-06, US-07
**Labels:** tech-task, squad:beta, EP-02
**Шар:** Domain

**Опис:**
Реалізувати CRDT (G-Counter або PN-Counter) для
синхронізації балансу квот після передачі. Гарантувати
eventual consistency при Mesh-синхронізації.

**Acceptance Criteria (технічні):**
- [ ] Merge двох станів не створює дублікатів
- [ ] Double-spending неможливий при конфлікті
- [ ] Працює offline (зберігає операцію локально)
- [ ] Автоматична синхронізація при відновленні Mesh

**Залежності:** #TT-01, #BAU-01
```

---

**Заголовок:** `TT-03 EP-02 US-06 | QR code generation for P2P transfer`

```
**Тип:** Technical Task
**Сквад:** Beta
**Пов'язані Stories:** US-06
**Labels:** tech-task, squad:beta, EP-02
**Шар:** Integration

**Опис:**
Генерація QR-коду з підписаними даними транзакції
для фізичного підтвердження (face-to-face).
QR містить: transaction_id, amount, sender_pub_key, signature.

**Acceptance Criteria (технічні):**
- [ ] QR генерується offline (без Mesh)
- [ ] Сканування підтверджує транзакцію
- [ ] Підпис верифікується через Web of Trust (EP-01)
- [ ] QR має TTL = 5 хвилин (захист від replay)

**Залежності:** #TT-01, #UF-01 (крок 6-7)
```

</details>

---

## INVEST-перевірка перед створенням

Перед створенням кожного TT перевірте за критеріями [INVEST](../03_requirements.md):

| Критерій | Питання | ✅ / ❌ |
| :--- | :--- | :--- |
| **I**ndependent | Чи може таска бути виконана незалежно від інших? | |
| **N**egotiable | Чи є простір для обговорення підходу? | |
| **V**aluable | Чи наближає до Sprint Goal? | |
| **E**stimable | Чи можна оцінити в SP? | |
| **S**mall | Чи вміщується в 1 спринт (≤ 13 SP)? | |
| **T**estable | Чи можна написати тест? | |

---

## Чеклист перед завершенням

- [ ] Вивчено User Stories, Domain Rules та User Flows
- [ ] Кожна Story декомпозована на 2–5 Technical Tasks
- [ ] Створено ≥ 5 тікетів `tech-task` у GitHub Project
- [ ] Кожен TT має шар (API / Domain / Data / Integration / UI)
- [ ] Кожен TT має Acceptance Criteria (чеклист)
- [ ] Пройдено INVEST-перевірку
- [ ] Всі тікети мають залежності та лейбли скваду

---

**[⬅️ Повернутися до плану воркшопу](plan.md)** | **[⬅️ Повернутися до головного меню курсу](../index.md)**
