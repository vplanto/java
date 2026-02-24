# Лекція 11: System Design Foundations. Чому код — це лише деталь

**Аудиторія:** 2-й курс (Junior Strong → майбутні Tech Leads)
**Ціль:** Зрозуміти різницю між написанням коду та проєктуванням систем. Навчитися бачити «невидимі» вимоги середовища виконання та свідомо приймати компроміси (trade-offs).

> **English version:** [English](en/11_system_design.md)

---

## 1. Відкрита дискусія (Warm-up)

**Питання 1:** Чим принципово відрізняється робота Senior Developer від Solution Architect?

<details markdown="1">
<summary>Розгорнути відповідь</summary>

Senior Developer фокусується на локальній оптимізації: Clean Code, SOLID, алгоритмічна складність всередині функції. KPI: Code Coverage, Cyclomatic Complexity. Горизонт планування: спринт–місяць.

Architect фокусується на глобальній оптимізації: TCO (Total Cost of Ownership), MTTD/MTTR, CAP-теорема. Рішення архітектора часто субоптимальні з точки зору коду, але вигідні для бізнесу. KPI: Scalability (RPS), Availability (SLA/SLO), Cost Efficiency. Горизонт планування: рік–5 років.

</details>

**Питання 2:** У вас ідеальний код, покритий тестами на 100%. Але при запуску в production система падає. Чому?

<details markdown="1">
<summary>Розгорнути відповідь</summary>

Проблема в Runtime Constraints. Unit-тести живуть у вакуумі. Production має:

- Noisy Neighbors: сусідні поди в K8s з'їли CPU (Throttling).
- Network Saturation: TCP Retransmits через поганий канал.
- Resource Limits: OOMKilled через неврахований оверхед JVM.
- Connection Exhaustion: закінчилися вільні дескриптори або порти в пулі БД.

</details>

**Питання 3:** Якщо AI може згенерувати код за секунди — навіщо архітектор?

<details markdown="1">
<summary>Розгорнути відповідь</summary>

AI генерує імплементацію, а не стратегію. Архітектор — це Director of Code, який:

1. Валідує дизайн на відповідність NFR.
2. Керує технічним боргом.
3. Приймає рішення в умовах невизначеності — там, де AI галюцинує.

</details>

---

## 2. Vibe Coding: інженер як директор

Vibe Coding — це не про «розслаблений кодинг». Це зміна ролі інженера з «будівельника» на «архітектора».

Старий підхід: ви думаєте про синтаксис, коми та дужки. Ваша швидкість обмежена швидкістю пальців.

Новий підхід: ви думаєте про Intent (намір) та Constraints (обмеження).

### Приклад: діалог з AI як інструментом

Запит: «Мені потрібен сервіс для обробки замовлень, який витримає 10k RPS.»

AI генерує простий REST-контролер з синхронним записом у БД.

Ваша відповідь (Validation & Hardening): «Ти використав синхронний запит до бази. При 10k RPS це створить Connection Storm. Перероби на асинхронну модель через Kafka. Додай Idempotency Key для захисту від дублів. Реалізуй Graceful Shutdown, щоб не губити повідомлення при редеплої.»

> Головне правило: ви не приймаєте код від AI, якщо не можете пояснити, як він поведе себе під навантаженням. AI — ваш Draftsman (кресляр), але підпис під кресленням ставите ви.

---

## 3. Code vs Execution Environment

Поширена помилка початківців: вважати, що вимоги приходять лише від бізнесу (функціонал). Архітектор знає: 50% вимог диктує середовище виконання (Runtime).

### Хибні припущення про розподілені системи (Fallacies of Distributed Computing)

Архітектори-початківці проєктують системи в ідеальному світі. Реальний — жорстокий.

**1. Мережа надійна.**
Реальність: TCP timeout, DNS failures, BGP rerouting.
Рішення: Idempotency (повтор безпечний), Dead Letter Queues (DLQ).

**2. Latency дорівнює нулю.**
Реальність: запит Київ–Нью-Йорк — ~120мс (фізика світла). N+1 запитів до бази через океан вб'ють UX.
Рішення: Data Locality, Caching (CDN, Redis), Batch requests.

**3. Пропускна здатність нескінченна.**
Реальність: 1Gbps канал легко забити нестиснутим JSON або логами.
Рішення: Protobuf/gRPC (бінарні формати), Compression (Gzip/Snappy), Pagination.

**4. Мережа безпечна.**
Реальність: Sniffing, MITM.
Рішення: mTLS (Mutual TLS), Zero Trust Architecture.

**5. Топологія не змінюється.**
Реальність: Pods in K8s — ephemeral (pets vs cattle). IP змінюються щохвилини.
Рішення: Service Discovery (Consul, K8s DNS), Client-side Load Balancing.

**6. Транспортна ціна нульова.**
Реальність: серіалізація/десеріалізація (Marshalling) коштує CPU.
Рішення: ефективні схеми, CPU profiling (Flamegraphs).

---

## 4. Методології дизайну

Два фундаментальних підходи до початку нового проєкту.

### Top-Down (Domain Driven Focus)

Філософія: «від потреби користувача до заліза».

1. User Needs & Ubiquitous Language: спілкування з бізнесом, виділення Bounded Contexts.
2. API Design (Contract First): OpenAPI — спочатку узгоджуємо інтерфейс, потім кодимо.
3. High-Level Components: C4 Model (Context → Container → Component).
4. Database & Infra: вибір CAP-tradeoffs (Consistency vs Availability).

Коли застосовувати: Greenfield-проєкти, складні бізнес-домени, де логіка важливіша за перформанс.

### Bottom-Up (Data Driven Focus)

Філософія: «від наявних можливостей до нових фіч».

1. Data & Capability: аналіз схеми даних, пропускної здатності шини, наявних логів.
2. Service Layer: патерн «Strangler Fig» — поступове винесення функціоналу з моноліту.
3. API та UI.

Коли застосовувати: Legacy Modernization, рефакторинг, жорсткі обмеження на залізо.

---

## 5. Карта архітектора: Північ, Південь, Схід-Захід

У складних розподілених системах легко загубитися. Архітектори використовують «сторони світу» для стандартизації потоків даних.

### Північ (Northbound): Вхідні двері

Публічний фасад. Середовище вороже — це Internet.

Клієнти: SPA, Mobile, IoT. Протоколи: HTTP/2 (REST), GraphQL, WebSockets.
Компоненти: API Gateway (Kong/Nginx), BFF (Backend for Frontend), WAF.
Архітектурний фокус: Security, Rate Limiting (Token Bucket), Caching (ETag, Cache-Control).

### Південь (Southbound): Фундамент і стан

Stateful-системи. Тут живе State.

Компоненти: RDBMS (Postgres з ACID), NoSQL (Cassandra/DynamoDB для high write throughput), Message Brokers (Kafka/RabbitMQ).
Архітектурний фокус: CAP Theorem, Connection Pooling (PgBouncer), Data Sharding.

### Схід-Захід (East-West): Внутрішня комунікація

Трафік між мікросервісами всередині VPC/Cluster.

Вимоги: High Throughput, Low Latency.
Протоколи: gRPC (Protobuf). Компоненти: Service Mesh (Istio/Linkerd).
Архітектурний фокус: Circuit Breaker, Bulkhead, Distributed Tracing.

---

## 6. Артефакти архітектора: за що платять

> Золоте правило: якщо рішення не задокументоване, його не існує.

### Architecture Blueprint (High-Level Design)

«Конституція» проєкту. Містить Tech Stack, NFR Values (RTO/RPO — скільки даних можемо втратити при аварії).

### ADR (Architecture Decision Records)

Журнал рішень. Формат:

```
Context:      Потрібно обрати брокер повідомлень.
Options:      Kafka vs RabbitMQ.
Decision:     Обираємо Kafka.
Consequences: Висока пропускна здатність, АЛЕ складніша інфраструктура
              та втрата гнучкого роутингу RabbitMQ.
```

Через рік ніхто не пам'ятатиме, чому обрали Kafka. ADR захищає від «ефекту нового менеджера».

### Impact Analysis Document

Таблиця залежностей «Service A залежить від API v1 Service B». Стратегія міграції: Blue-Green, Canary або Big Bang (тільки якщо іншого виходу немає).

### Rollout Checklist (Runbook)

Pre-deployment: schema migration. Verification: `/health` повертає 200. Rollback: `helm rollback`.

---

## 7. Візуалізація: C4 Model

Архітектор говорить з різними аудиторіями. C4 Model — стандарт індустрії.

**Рівень A: Logical Design («What»).** Аудиторія — бізнес, PM. Показуємо Value Streams: Клієнт → Замовлення → Оплата → Доставка. Жодних серверів, лише бізнес-сутності.

**Рівень Б: Physical Design («Where»).** Аудиторія — DevOps, SRE. Топологія розгортання: «Замовлення» → K8s Deployment (3 replicas, 200m CPU), «Оплата» → External API Stripe, зв'язок — Ingress Controller, TLS v1.3.

---

## 8. Ціна архітектурної помилки: Post-Mortem

Архітектор приймає найдорожчі рішення до написання першого рядка коду. Помилка на етапі дизайну коштує в 100 разів дорожче за баг у коді.

### Ticketmaster: Distributed Locking fail

При продажу квитків тисячі користувачів намагались купити одне місце одночасно. Optimistic locking на рівні БД не витримав — база впала від кількості ROLLBACK-транзакцій.

Урок: для High Contention ресурсів (квитки, складські залишки) потрібна черга або in-memory серіалізація запитів, а не прямий запис у БД.

### Mars Climate Orbiter ($327M)

Метрична система vs Імперська. Відсутність Contract Testing і типізації даних на рівні API-схем. Інтерфейси мають бути суворо типізовані (Strongly Typed IDL).

### Cascading Failures (ефект доміно)

Впав сервіс рекомендацій — головна сторінка магазину теж перестала вантажитися. Причина: відсутність Timeouts і жорстка залежність (Strong Coupling).

Урок: Graceful Degradation. Якщо рекомендації недоступні — показуй «Топ продажів» або порожній блок, але дай користувачеві купити товар.

---

## 9. Екзаменаційний пул (Exam Questions)

**Питання 1:** Чому «швидко» — погана вимога? Як архітектор має сформулювати вимогу до швидкодії?

<details markdown="1">
<summary>Еталонна відповідь</summary>

«Швидко» — суб'єктивно і не можна поміряти. Архітектор каже: «P99 Latency < 200ms при 10k RPS». Це означає, що 99% запитів будуть швидші за 200мс навіть під навантаженням.

</details>

**Питання 2:** Ви змінюєте структуру бази даних білінгу. Який документ готуєте першим?

<details markdown="1">
<summary>Еталонна відповідь</summary>

Impact Analysis (кого зачепить зміна) і ADR (чому ми це робимо і як мігруємо безпечно).

</details>

**Питання 3:** До якої «сторони світу» відноситься Load Balancer?

<details markdown="1">
<summary>Еталонна відповідь</summary>

Northbound. Це щит системи — перший компонент, що приймає зовнішній трафік.

</details>

**Питання 4:** Команда каже: «Не будемо обробляти таймаути мережі, бо у нас надійний дата-центр». Яку помилку вони роблять?

<details markdown="1">
<summary>Еталонна відповідь</summary>

Хибне припущення «Network is reliable» — перший Fallacy of Distributed Computing. Внутрішні мережі також не мають 100% надійності. Потрібно впроваджувати Retry з Jitter та Circuit Breaker для кожного мережевого виклику.

</details>

---

**[⬅️ Лекція 10: Distributed Systems](10_distributed_systems.md)** | **[Лекція 12: Refactoring ➡️](12_refactoring.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
