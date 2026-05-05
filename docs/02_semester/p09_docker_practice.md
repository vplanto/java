# Практикум P09: Docker на практиці. Пакуємо і запускаємо сервіс

**Аудиторія:** 2-й курс (Junior Strong)
**Тип:** Hands-on Lab
**Попередні вимоги:** [Лекція 9: Docker](09_docker.md), Spring Boot сервіс (P03–P05)

> **English version:** [English](en/p09_docker_practice.md)

---

## Частина 1: Dockerfile для Spring Boot (20 хв)

### Бізнес-сценарій: "Works on my machine"
У вас на ноутбуці стоїть Java 21 і база даних на порту 5432. Ви пишете код, віддаєте колезі, а у нього Java 17 і база зайнята іншим проєктом. Результат — сервіс не стартує.
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

### Бізнес-сценарій: Database persistence
Наш сервіс не існує у вакуумі. Йому потрібна база даних PostgreSQL. Якби ми використовували звичайний Docker, нам довелося б вручну запускати контейнер з БД, налаштовувати мережу, а потім запускати застосунок. 
`docker-compose` дозволяє описати всю інфраструктуру проєкту (App + Database) в одному файлі та підняти її однією командою.

### Завдання 2.1: Створення docker-compose.yml
Напишіть `docker-compose.yml` у корені проєкту.
Ми додаємо правильний `depends_on` (щоб аплікація чекала, поки БД буде готова приймати запити) та `volumes` (щоб дані не зникали при перезапуску контейнера).

**Файл: docker-compose.yml**
```yaml
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

### Завдання 2.2: Секрети та змінні середовища
У `docker-compose.yml` ми використовували `${DB_PASSWORD}`. Де його взяти?

**Файл: .env (НІКОЛИ НЕ ПУШИТИ В GIT!)**
```properties
DB_PASSWORD=supersecretpassword
```

**Файл: .env.example (Для Git, як документація)**
```properties
DB_PASSWORD=your-database-password-here
```

> [!CAUTION]
> **Секрети у Git:** Якщо ви випадково закомітите `.env` з реальними паролями на GitHub — це інцидент безпеки. Використовуйте `.gitignore`.

### Завдання 2.3: Корисні команди для відлагодження
Запустіть середовище (`docker compose up -d`) та виконайте ці команди. Вони вам знадобляться на щоденній основі:

**Оточення: Термінал**
```bash
# Статус всіх сервісів: ім'я, стан, порти
docker compose ps

# Стрімінг логів з сервісу app (Ctrl+C для виходу)
docker compose logs app --follow

# Зайти в контейнер db і показати всі таблиці в БД library
docker compose exec db psql -U library_user -d library -c "\dt"

# Live-моніторинг CPU/RAM/Network по всіх контейнерах
docker stats
```

---

## Частина 3: Налагодження типових проблем (15 хв)

### Завдання 3.1: Діагностика інцидентів
Ось реальні ситуації з якими ви зіткнетесь у Production. Як їх вирішити?

> [!WARNING]
> **Сценарій A: App стартує, але падає з `Connection refused to db:5432`**
> **Причина:** База даних ще не готова приймати з'єднання, але Spring Boot вже намагається виконати міграції.
> **Рішення:** Перевірити логи бази (`docker compose logs db`) та переконатись, що `depends_on` має `condition: service_healthy`, а healthcheck бази налаштований правильно.

> [!WARNING]
> **Сценарій B: App працює в Docker, але з хоста `:8080` дає `Connection refused`**
> **Причина:** Порт не відкритий назовні, або Spring Boot слухає лише `127.0.0.1` всередині контейнера.
> **Рішення:** Перевірити мапінг портів `ports: - "8080:8080"` у Compose. Зайти в контейнер (`docker compose exec app /bin/sh -c "ss -tlnp"`) і перевірити, чи сервер слухає `0.0.0.0`.

> [!WARNING]
> **Сценарій C: Після оновлення `.env` app все одно використовує старий пароль**
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

3. **Secrets:** Ваш колега захотів передати DB_PASSWORD прямо у Dockerfile: `ENV DB_PASSWORD=secret123`. Яка проблема і як правильно?

<details markdown="1">
<summary>Відповідь</summary>

Проблема: пароль назавжди вбудований у image. Видно через `docker inspect`. Залишається в history шарів навіть після `RUN unset DB_PASSWORD`. Image з паролем можна випадково запушити в публічний Registry.

Правильно: передавати тільки при `docker run -e` або через `.env` файл в Compose. У production — через Kubernetes Secrets, AWS Secrets Manager або Docker Swarm Secrets. У Dockerfile не повинно бути жодних значень — тільки `ENV DB_PASSWORD=""` як документація.

</details>

---

**[⬅️ Лекція 9: Docker](09_docker.md)** | **[P07: API Practice ➡️](p07_api_practice.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
