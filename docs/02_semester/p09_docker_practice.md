# Практикум P09: Docker на практиці. Пакуємо і запускаємо сервіс

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 9: Docker](09_docker.md), Spring Boot сервіс (P03–P05)

> **English version:** [English](en/p09_docker_practice.md)

---

## Мета заняття

Запакувати готовий Spring Boot сервіс у Docker-образ, підняти його разом з PostgreSQL через Docker Compose і переконатись, що «it works on every machine».

---

## Частина 1: Dockerfile для Spring Boot (20 хв)

### Вправа 1.1: Що не так у цьому Dockerfile?

```dockerfile
FROM openjdk:8
COPY . /app
WORKDIR /app
RUN mvn package
EXPOSE 8080
CMD java -jar target/app.jar
```

Знайдіть щонайменше 4 проблеми.

<details markdown="1">
<summary>Відповідь</summary>

| # | Проблема | Чому це погано | Як виправити |
|---|---|---|---|
| 1 | `openjdk:8` | Java 8 — end-of-life. Немає security-патчів | `eclipse-temurin:21-jre` |
| 2 | `COPY . /app` разом з `RUN mvn package` | Не використовується кешування. При зміні будь-якого файлу — перезавантажує всі залежності | Multi-stage з окремим `COPY pom.xml` |
| 3 | JDK у runtime-образі | Образ важить ~700MB. У prod JDK не потрібен | Multi-stage: builder (JDK) + runtime (JRE) |
| 4 | `CMD java -jar` без масиву | При такій формі Java не отримує SIGTERM → нема graceful shutdown | `ENTRYPOINT ["java", "-jar", "app.jar"]` |

</details>

### Вправа 1.2: Напишіть правильний Dockerfile

Напишіть оптимальний Dockerfile для Spring Boot сервісу з урахуванням:
- Multi-stage build
- Кешування залежностей
- Мінімальний розмір образу
- Graceful shutdown

<details markdown="1">
<summary>Рішення</summary>

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

# Не root-користувач — hardening
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

EXPOSE 8080

# Масив-форма → PID 1 отримує SIGTERM від Docker
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
```

`-XX:+UseContainerSupport` — JVM читає cgroup-ліміти, а не RAM хоста.
`-XX:MaxRAMPercentage=75.0` — JVM використовує 75% виділеної пам'яті контейнера.

</details>

---

## Частина 2: Docker Compose (25 хв)

### Вправа 2.1: Запуск з PostgreSQL

Напишіть `docker-compose.yml` для Library-сервісу з:
- Spring Boot app
- PostgreSQL 15
- Правильним `depends_on` з health check
- Persistence volume для БД

<details markdown="1">
<summary>Рішення</summary>

```yaml
# docker-compose.yml
version: "3.9"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/library
      SPRING_DATASOURCE_USERNAME: library_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped
    # Resource limits
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: '0.5'

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: library
      POSTGRES_USER: library_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql  # початкова схема
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U library_user -d library"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s
    restart: unless-stopped

volumes:
  postgres_data:
```

`.env` файл (не в Git!):
```
DB_PASSWORD=supersecretpassword
```

`.env.example` (в Git, для документування):
```
DB_PASSWORD=your-database-password-here
```

</details>

### Вправа 2.2: Корисні команди для відлагодження

Виконайте ці команди для запущеного Compose і поясніть, що кожна показує:

```bash
docker compose ps
docker compose logs app --follow
docker compose exec db psql -U library_user -d library -c "\dt"
docker compose exec app /bin/sh -c "java -version"
docker stats
```

<details markdown="1">
<summary>Пояснення</summary>

```bash
# Статус всіх сервісів: ім'я, стан, порти
docker compose ps

# Стрімінг логів з сервісу app (Ctrl+C для виходу)
docker compose logs app --follow

# Зайти в контейнер db і показати всі таблиці в БД library
docker compose exec db psql -U library_user -d library -c "\dt"

# Перевірити Java-версію всередині app-контейнера
docker compose exec app /bin/sh -c "java -version"

# Live-моніторинг CPU/RAM/Network по всіх контейнерах
docker stats
```

</details>

---

## Частина 3: Налагодження типових проблем (15 хв)

### Вправа 3.1: Діагностика

Для кожної ситуації — знайдіть причину і команду для діагностики:

**Сценарій A:** App стартує, але не може підключитись до БД. Logs: `Connection refused to db:5432`.

**Сценарій B:** App стартує, але відповідає на `:8080` — `Connection refused` з хоста.

**Сценарій C:** После оновлення `.env` — app все одно використовує старий пароль.

<details markdown="1">
<summary>Відповіді</summary>

**Сценарій A:** DB ще не готова або health check не проходить.
```bash
docker compose logs db        # подивитись на помилки PostgreSQL
docker compose ps             # перевірити статус: чи db (healthy)?
# Якщо depends_on з condition: service_healthy не спрацював — revise healthcheck
```

**Сценарій B:** Порт не відкритий або binding на 127.0.0.1 замість 0.0.0.0.
```bash
docker compose exec app /bin/sh -c "ss -tlnp"   # слухає чи на 0.0.0.0:8080?
# Перевірити EXPOSE та ports у docker-compose.yml
# Spring Boot: server.address=0.0.0.0 (дефолт, не змінювати на localhost)
```

**Сценарій C:** Compose кешує environment. Треба перезапустити з `--env-file`.
```bash
docker compose down
docker compose --env-file .env up -d
# або
docker compose up -d --force-recreate
```

</details>

---

## Частина 4: Оптимізація образу (10 хв)

### Вправа 4.1: Порівняйте розміри

```bash
# Збудуйте образ і порівняйте розміри
docker build -t library-app:naive -f Dockerfile.naive .
docker build -t library-app:optimized -f Dockerfile .
docker images | grep library-app
```

Типові результати:

| Образ | Базовий | Розмір |
|---|---|---|
| Naive (JDK, без multi-stage) | `openjdk:8` | ~650 MB |
| Optimized (JRE, multi-stage) | `eclipse-temurin:21-jre` | ~220 MB |
| Ultra-minimal (distroless) | `gcr.io/distroless/java21` | ~130 MB |

Менший образ:
- швидше завантажується з Registry при деплої
- менша поверхня атаки (немає зайвих утиліт)
- дешевший Registry storage

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

3. **Secrets:** Ваш колега захотів передати DB_PASSWORD прямо у Dockerfile: `ENV DB_PASSWORD=secret123`. Яка проблема і як правильно?

<details markdown="1">
<summary>Відповідь</summary>

Проблема: пароль назавжди вбудований у image. Видно через `docker inspect`. Залишається в history шарів навіть після `RUN unset DB_PASSWORD`. Image з паролем можна випадково запушити в публічний Registry.

Правильно: передавати тільки при `docker run -e` або через `.env` файл в Compose. У production — через Kubernetes Secrets, AWS Secrets Manager або Docker Swarm Secrets. У Dockerfile не повинно бути жодних значень — тільки `ENV DB_PASSWORD=""` як документація.

</details>

---

**[⬅️ Лекція 9: Docker](09_docker.md)** | **[P07: API Practice ➡️](p07_api_practice.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
