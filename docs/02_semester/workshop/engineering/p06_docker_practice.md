# Практикум P06: Docker на практиці. Пакуємо і запускаємо сервіс

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 9: Docker](../../09_docker.md), Spring Boot сервіс (P03–P05)

> **English version:** [English](en/p06_docker_practice.md)

---

## Частина 1: Dockerfile для Spring Boot (20 хв)

### Бізнес-сценарій: "Works on my machine"
У вас на ноутбуці стоїть Java 21 і ви пишете код. Ви віддаєте його колезі, а у нього лише Java 17, або на сервері взагалі не встановлена Java. Результат — сервіс не стартує.
Щоб це вирішити, ми пакуємо наш застосунок разом із його оточенням (JRE) у Docker-образ. Тепер сервіс гарантовано запуститься на будь-якому комп'ютері або сервері.

### Завдання 1.1: Аналіз поганого Dockerfile (Антипатерни)
Подивіться на цей код. Це типовий Dockerfile новачка.

```dockerfile
FROM openjdk:8
COPY . /app
WORKDIR /app
RUN mvn package
EXPOSE 8080
CMD java -jar target/app.jar
```

> [!CAUTION]
> **Чому цей код ніколи не пройде Code Review:**
> 1. **Застаріла база:** `openjdk:8` — це end-of-life Java 8 без патчів безпеки.
> 2. **Відсутність кешування:** `COPY . /app` та `RUN mvn package` в одному шарі означає, що зміна одного `.java` файлу призведе до повторного завантаження всього інтернету (залежностей Maven).
> 3. **Гігантський розмір:** У фінальному образі лежить JDK (компілятор). Образ буде важити ~700MB, хоча для запуску достатньо JRE (~200MB).
> 4. **Немає Graceful Shutdown:** `CMD java -jar` без масиву `[]` запускається через shell. Контейнер не отримає сигнал SIGTERM при зупинці, і ваші користувачі отримають помилки під час деплою.

### Завдання 1.2: Створення оптимального Dockerfile
Створіть файл `Dockerfile` у корені вашого проєкту. Ми використаємо **Multi-stage build**: на першому етапі компілюємо код (потрібен важкий JDK), на другому — пакуємо лише готовий `jar` (потрібен легкий JRE).

**Файл: Dockerfile**
```dockerfile
# ── Stage 1: Build ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Копіюємо лише pom.xml спочатку → шар кешується якщо pom не змінився
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -q

# Тепер копіюємо код → цей шар інвалідується тільки при зміні src
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копіюємо лише jar з builder-стейджу
COPY --from=builder /build/target/*.jar app.jar

# Security hardening: Не запускаємо від root
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

EXPOSE 8080

# Масив-форма → PID 1 отримує SIGTERM від Docker
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
```

> [!TIP]
> **Секрети JVM у Docker:**
> Зверніть увагу на прапорці `-XX:+UseContainerSupport` та `-XX:MaxRAMPercentage=75.0`. Без них Java буде дивитись на оперативну пам'ять хоста (наприклад, 16 ГБ), виділить гігантський Heap, перевищить ліміти контейнера і буде вбита системою (OOMKilled).

---

## Частина 2: Docker Compose (25 хв)

### Бізнес-сценарій: Infrastructure as Code
Навіть якщо наш сервіс працює in-memory і не має бази даних, запускати його через `docker run -p 8080:8080 -e APP_LIBRARY_MAX_BOOKS=50 -d library-app` дуже незручно, особливо коли параметрів стає багато. 
`docker-compose` дозволяє описати всю інфраструктуру проєкту (порти, змінні середовища, ліміти пам'яті) в одному файлі та підняти її однією командою. Це підхід **"Infrastructure as Code"**.

### Завдання 2.1: Створення docker-compose.yml
Напишіть `docker-compose.yml` у корені проєкту.

**Файл: docker-compose.yml**
```yaml
version: "3.9"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      APP_LIBRARY_MAX_BOOKS: ${MAX_BOOKS}
    restart: unless-stopped
    # Resource limits
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: '0.5'
```

### Завдання 2.2: Змінні середовища
У `docker-compose.yml` ми використовували `${MAX_BOOKS}`. Де його взяти?

**Файл: .env (НІКОЛИ НЕ ПУШИТИ В GIT!)**
```properties
MAX_BOOKS=150
```

**Файл: .env.example (Для Git, як документація)**
```properties
MAX_BOOKS=50
```

> [!CAUTION]
> **Секрети у Git:** Якщо ви колись додасте в `.env` реальні паролі або токени доступу, і випадково закомітите цей файл на GitHub — це інцидент безпеки. Використовуйте `.gitignore`.

### Завдання 2.3: Корисні команди для відлагодження
Запустіть середовище (`docker compose up -d`) та виконайте ці команди. Вони вам знадобляться на щоденній основі:

**Оточення: Термінал**
```bash
# Статус всіх сервісів: ім'я, стан, порти
docker compose ps

# Стрімінг логів з сервісу app (Ctrl+C для виходу)
docker compose logs app --follow

# Зайти в контейнер app і переглянути його файлову систему
docker compose exec app sh

# Live-моніторинг CPU/RAM/Network по всіх контейнерах
docker stats
```

---

## Частина 3: Налагодження типових проблем (15 хв)

### Завдання 3.1: Діагностика інцидентів
Ось реальні ситуації з якими ви зіткнетесь у Production. Як їх вирішити?

> [!WARNING]
> **Сценарій A: App стартує і падає з помилкою `OOMKilled`**
> **Причина:** Контейнер перевищив ліміт пам'яті, вказаний у docker-compose (наприклад, `512m`).
> **Рішення:** Перевірити налаштування `MAX_RAM_PERCENTAGE` у `Dockerfile` (чи воно взагалі є) та збільшити ліміт пам'яті у Compose файлі.

> [!WARNING]
> **Сценарій B: App працює в Docker, але з хоста `:8080` дає `Connection refused`**
> **Причина:** Порт не відкритий назовні, або Spring Boot слухає лише `127.0.0.1` всередині контейнера.
> **Рішення:** Перевірити мапінг портів `ports: - "8080:8080"` у Compose. Зайти в контейнер (`docker compose exec app /bin/sh -c "ss -tlnp"`) і перевірити, чи сервер слухає `0.0.0.0`.

> [!WARNING]
> **Сценарій C: Після оновлення `.env` app все одно використовує старий ліміт книг**
> **Причина:** Docker Compose кешує контейнери, якщо конфігурація YAML не змінилась.
> **Рішення:** Перезапустити явно: `docker compose down` та `docker compose up -d` (або `docker compose up -d --force-recreate`).

---

## Частина 4: Оптимізація та FinOps (10 хв)

### Завдання 4.1: Порівняння розмірів образів
Розмір Docker-образу безпосередньо впливає на вартість хмарного сховища (Registry) та швидкість завантаження сервісу (Cold Start).

**Оточення: Термінал**
```bash
# Збудуйте образ і порівняйте розміри (якщо у вас є naive Dockerfile)
docker build -t library-app:naive -f Dockerfile.naive .
docker build -t library-app:optimized -f Dockerfile .
docker images | grep library-app
```

> [!TIP]
> **FinOps (Економіка хмари)**
> 
> | Образ | Базовий | Розмір |
> |---|---|---|
> | Naive (JDK, без multi-stage) | `openjdk:8` | ~650 MB |
> | Optimized (JRE, multi-stage) | `eclipse-temurin:21-jre` | ~220 MB |
> | Ultra-minimal (distroless) | `gcr.io/distroless/java21` | ~130 MB |
> 
> Менший образ це не лише про гроші. Це **швидший деплой** та **менша поверхня атаки** (у distroless образах немає навіть `sh` або `bash`, тому хакер не зможе виконати шелл-команди, якщо зламає ваш додаток).

---

## Контрольні питання

1. **cgroups:** Ви задали `memory: 512m` у Compose, але JVM все одно споживає 700 MB і контейнер падає з OOMKilled. Чому, і як виправити?

<details markdown="1">
<summary>Відповідь</summary>

JVM за замовчуванням читає RAM хоста, а не cgroup-ліміт. На сервері з 16 GB RAM JVM виділить GC-хіп ~4 GB, ігноруючи Docker memory limit.

Виправлення: прапори `-XX:+UseContainerSupport` (ввімкнено за замовчуванням у JDK 11+) і `-XX:MaxRAMPercentage=75.0`. Тоді JVM читає cgroup і виділяє 75% від 512 MB = ~384 MB. Залишок — для Metaspace, стеків і OS.

</details>

2. **Layer caching:** Ви змінили лише `README.md` у проєкті. Скільки Docker-шарів буде перезбудовано при `docker build`?

<details markdown="1">
<summary>Відповідь</summary>

У правильно написаному Dockerfile:
```
COPY pom.xml .           ← не змінився → кеш
RUN mvn dependency:go-offline  ← кеш
COPY src ./src           ← не змінився → кеш
RUN mvn package          ← кеш
COPY --from=builder ...  ← кеш
```

README.md не потрапляє в контекст якщо він у `.dockerignore`. Якщо немає `.dockerignore` — шар `COPY src` інвалідується. Тому важливо мати `.dockerignore`:
```
.git
README.md
*.md
target/
```

</details>

3. **Secrets:** Ваш колега захотів передати API_TOKEN прямо у Dockerfile: `ENV API_TOKEN=secret123`. Яка проблема і як правильно?

<details markdown="1">
<summary>Відповідь</summary>

Проблема: токен назавжди вбудований у image. Видно через `docker inspect`. Залишається в history шарів навіть після `RUN unset API_TOKEN`. Image з токеном можна випадково запушити в публічний Registry.

Правильно: передавати тільки при `docker run -e` або через `.env` файл в Compose. У production — через Kubernetes Secrets, AWS Secrets Manager або хмарні Environment Variables (як у Render.com). У Dockerfile не повинно бути жодних значень — тільки `ENV API_TOKEN=""` як документація.

</details>

---

**[⬅️ Лекція 9: Docker](../../09_docker.md)** | **[P08: API Practice ➡️](p08_api_practice.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
