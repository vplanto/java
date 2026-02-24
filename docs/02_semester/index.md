# Java Engineering. Семестр 2: Від Вимог до Розподілених Систем

> **English version:** [English](en/index.md)

**Рівень:** Middle / Architect Foundation
**Філософія:** Vibe Coding — ми проєктуємо рішення, а AI пише код. [Методологія](vibe_coding.md)

Семестр охоплює повний цикл інженерії: від розуміння вимог (включно з NFRs) до побудови розподілених систем і деплою в хмару.

**Мета:** Зламати парадигму «головне, щоб код компілювався». Перейти від локальної оптимізації (функції) до глобальної (системи). Навчитись бачити «невидимі» вимоги (NFR), оцінювати вартість рішень і будувати системи, що виживають у Production.

---

## Theory Track — Лекції

Матеріали в рекомендованому порядку проходження.

### Блок 1: Процеси та Вимоги
*Як зрозуміти, що будувати, до того, як відкрити IDE.*

| # | Лекція | Ключові теми |
|---|---|---|
| 0 | **[Intro. Engineering Mindset](00_intro.md)** | Від кодера до інженера. Код як пасив, архітектура як актив |
| 1 | **[SDLC](01_sdlc.md)** | Етапи життя ПЗ. Роль Jira, Git, CI/CD |
| 2 | **[Delivery Methodology](02_delivery_methodology.md)** | Agile, Scrum, Kanban vs Waterfall |
| 3 | **[Definition of Requirements](03_requirements.md)** | Функціональні вимоги. User Stories. Acceptance Criteria |

### Блок 2: Якість та Архітектурні Характеристики
*Найважливіша частина для архітектора.*

| # | Лекція | Ключові теми |
|---|---|---|
| 4 | **[NFRs](04_nfr.md)** | Scalability, Availability, Security. Чому «швидко» — погана вимога |
| 5 | **[NFR Discovery](05_nfr_late_discovery.md)** | Ціна пізнього виявлення NFR. ADR. NFR-аудит коду |
| 6 | **[QA Strategy](06_qa_strategy.md)** | Піраміда тестування. SonarQube. Quality Gates у пайплайні |

### Блок 3: Технічна Реалізація
*Як це будується руками.*

| # | Лекція | Ключові теми |
|---|---|---|
| 7 | **[API Design](07_api_design.md)** | REST, HTTP Codes. Контракти та версіонування. OpenAPI |
| 8 | **[Test Cases & Coverage](08_test_cases.md)** | BVA, Decision Table. Unit vs Integration. Testcontainers |
| 9 | **[Docker & Containerization](09_docker.md)** | Dockerfile, Multi-stage build, cgroups, Docker Compose |

### Блок 4: Розподілені Системи та System Design
*Архітектура великих систем.*

| # | Лекція | Ключові теми |
|---|---|---|
| 10 | **[Distributed Systems](10_distributed_systems.md)** | Fallacies, CAP-теорема, Circuit Breaker, Observability, K8s |
| 11 | **[System Design Foundations](11_system_design.md)** | Mindset архітектора, ADR, C4 Model, Trade-offs |
| 12 | **[Refactoring](12_refactoring.md)** | Code Smells, Technical Debt, TDD-цикл, Legacy Code, Extract Method |
| — | **[Case Study: CAP Theorem](case_study_cap.md)** | Практичні приклади CP vs AP. FinOps-вимір консистентності |

---

## Hands-on Track — Практикуми

Паралельний трек, де теорія перетворюється на код.

### System Design

| Файл | Зміст |
|---|---|
| **[P01: URL Shortener — від нуля до архітектури](p01_system_design_workshop.md)** | Live Design Session: Requirements, Base62, Data Design, FinOps |

### Технічні практикуми

| Файл | Лекція | Зміст |
|---|---|---|
| **[P07: API Design на практиці](p07_api_practice.md)** | Л7 | URL-структура, HTTP-статуси, OpenAPI YAML, Breaking Changes |
| **[P08: Testing на практиці](p08_testing_practice.md)** | Л8 | Unit (Mockito), @DataJpaTest, MockMvc, Coverage |
| **[P09: Docker на практиці](p09_docker_practice.md)** | Л9 | Dockerfile, Docker Compose + PostgreSQL, Debugging |

### Spring Boot Ecosystem
*Від фізики Web-процесів до автоматичного деплою в Production.*

| Файл | Зміст |
|---|---|
| **[P02: Архітектура сучасних Web-застосунків](p02_spring_web_arch.md)** | SSR vs SPA, IoC Container, Spring Boot Philosophy |
| **[P03: Zero to Hero — перший Spring Boot сервіс](p03_spring_zero_to_hero.md)** | Spring Initializr, Embedded Tomcat, REST Controller, DTO |
| **[P04: Архітектурна гігієна та DI](p04_spring_architecture_di.md)** | Service Layer, Constructor Injection, Separation of Concerns |
| **[P05: Production Ready](p05_spring_production_ready.md)** | External Config, Global Exception Handling, HTTP Status Codes |
| **[P06: Hello Cloud — деплой у хмару](p06_spring_cloud_deployment.md)** | PaaS (Render.com), CI/CD, Cold Start, Environment Variables |

---

## Definition of Done

Після проходження семестру студент повинен вміти:

1. Створити REST API з нуля без підглядання в Google «how to start spring boot».
2. Пояснити, чому Constructor Injection, а не `@Autowired` на полях.
3. Реалізувати чисту обробку помилок (JSON з описом проблеми).
4. Змінювати бізнес-параметри без перекомпіляції коду.
5. Розгорнути сервіс у хмарі (HTTPS) з CI/CD.
6. Спроєктувати систему з урахуванням NFRs і CAP-теореми.

---

## Технічні вимоги

| Інструмент | Вимога |
|---|---|
| JDK | 17 або 21 |
| IDE | IntelliJ IDEA (Community або Ultimate) |
| Build Tool | Maven |
| API Client | Postman або `curl` |
| Cloud | Render.com (Free Tier) |
| VCS | GitHub |

---

## Екзамен

- **Теорія:** NFRs, SDLC, CAP-теорема, Docker, System Design.
- **Практика:** Code Review Spring Boot сервісу + захист архітектурного рішення (System Design Interview).

---

## Додатково

- **[Глосарій термінів](glossary.md)** — всі незнайомі слова з лекцій: Jira, CI/CD, Sprint, Backlog, Docker і ще 40+.
- **[todo_2026.md](todo_2026.md)** — майбутні теми та плани розвитку курсу.

---

**[⬅️ Повернутися до головного меню курсу](../index.md)**
