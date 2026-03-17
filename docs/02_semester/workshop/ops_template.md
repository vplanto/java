# Ops / DevOps: Інфраструктура та середовища

**Проєкт:** VARTA (Distributed Resilience Orchestrator)
**Сквад:** Alpha / Beta / Gamma *(вказати свій)*
**GitHub Project:** [VARTA Board](https://github.com/users/vplanto/projects/1)
**Зв'язок:** [Product Backlog](product_backlog.md) | [План воркшопу](plan.md) | [Dev](dev_template.md)

---

## Ціль ролі Ops

Ви — **інженер інфраструктури**. Ваше завдання — визначити, **які середовища, пайплайни та нефункціональні вимоги** потрібні, щоб код команди Dev працював у production.

Кожна інфраструктурна задача — це **окремий тікет** у [GitHub Project](https://github.com/users/vplanto/projects/1). Тікет = ваш дизайн-артефакт.

> 📝 **Naming Convention:** Див. [Конвенція оформлення тікетів](plan.md#-конвенція-оформлення-тікетів-github-project). Заголовок англійською, тіло українською.

---

## Робочий процес

### Крок 1: Вивчіть вхідні артефакти

- **Product Backlog** → [Обмеження та Constraints](product_backlog.md#обмеження-та-constraints) (Offline-first, Mesh Transport, Hardware, Енергоефективність).
- **Dev тікети** → `TT-*` — які сервіси/компоненти створюються і де вони будуть запускатись.
- **SME тікети** → `BAU-*` — які процеси мають працювати безперебійно.

### Крок 2: Визначте інфраструктурні потреби

Для кожного Technical Task від Dev визначте:
- **Де** це буде запускатись? (Mesh node / Edge device / Cloud fallback)
- **Як деплоїти** оновлення? (OTA / Manual / USB)
- **Як моніторити** здоров'я? (Health check / Telemetry)
- **Які NFR** (Non-Functional Requirements) стосуються цього компонента?

### Крок 3: Створіть тікети у GitHub Project

---

## Типи тікетів

### 🏷 `OPS` — Ops Task (Інфраструктурна задача)

**Формат заголовку:** `OPS-## EP-XX US-YY | Short English description`

**Тіло тікету (українською):**
```
**Тип:** Ops Task
**Сквад:** Alpha / Beta / Gamma
**Пов'язані Stories:** US-XX
**Labels:** ops, squad:xxx, EP-XX
**Категорія:** Environment / CI-CD / Monitoring / NFR / Security

**Опис:**
<Що потрібно забезпечити з боку інфраструктури>

**NFR / Constraint:**
<Яку нефункціональну вимогу або обмеження закриває ця задача>

**Acceptance Criteria:**
- [ ] <Критерій 1>
- [ ] <Критерій 2>

**Залежності:** #<номер пов'язаного тікету>
```

---

### Категорії Ops-тікетів

#### 🌐 Environment — Середовища

Визначте, де і як запускаються компоненти VARTA.

<details>
<summary>📋 Приклад: Environment</summary>

**Заголовок:** `OPS-01 EP-03 US-13 | Telemetry buffer storage on edge device`

```
**Тип:** Ops Task
**Сквад:** Alpha
**Пов'язані Stories:** US-13
**Labels:** ops, squad:alpha, EP-03
**Категорія:** Environment

**Опис:**
Визначити стратегію зберігання телеметричного буфера
на edge-пристрої (Raspberry Pi 3B+ з 1 GB RAM).
При розриві Mesh дані зберігаються локально.

**NFR / Constraint:**
- Hardware: RPi 3B+, 1 GB RAM, microSD 16 GB
- Буфер не повинен займати > 100 MB
- Ротація: FIFO, видаляти дані старші за 72 год

**Acceptance Criteria:**
- [ ] Обрано формат зберігання (SQLite / flat file / ring buffer)
- [ ] Визначено ліміт розміру та стратегію ротації
- [ ] Дані не втрачаються при аварійному вимкненні (fsync)
- [ ] Автоматична синхронізація при відновленні Mesh

**Залежності:** #TT-* (Telemetry buffer), #BAU-01
```
</details>

#### 🔄 CI/CD — Пайплайн розгортання

Як код потрапляє на пристрої.

<details>
<summary>📋 Приклад: CI/CD</summary>

**Заголовок:** `OPS-02 EP-01 US-01 | OTA update pipeline for Mesh nodes`

```
**Тип:** Ops Task
**Сквад:** Gamma
**Пов'язані Stories:** US-01
**Labels:** ops, squad:gamma, EP-01
**Категорія:** CI-CD

**Опис:**
Визначити стратегію OTA (Over-The-Air) оновлень
для вузлів Mesh-мережі. Оновлення мають
розповсюджуватися через Mesh (без інтернету).

**NFR / Constraint:**
- Offline-first: оновлення поширюється peer-to-peer
- Mesh Transport: ≤ 250 Kbps — потрібно мінімізувати розмір пакету
- Rollback: якщо оновлення зламало вузол — автоматичний відкат

**Acceptance Criteria:**
- [ ] Визначено формат update package (delta / full)
- [ ] Описано механізм поширення (store-and-forward)
- [ ] Є стратегія rollback при невдалому оновленні
- [ ] Оновлення підписується (tamper-proof)

**Залежності:** #TT-* (Mesh Identity)
```
</details>

#### 📊 Monitoring — Моніторинг та здоров'я

Як дізнатись, що система працює.

<details>
<summary>📋 Приклад: Monitoring</summary>

**Заголовок:** `OPS-03 EP-03 US-12 | Health check for critical IoT sensors`

```
**Тип:** Ops Task
**Сквад:** Alpha
**Пов'язані Stories:** US-12
**Labels:** ops, squad:alpha, EP-03
**Категорія:** Monitoring

**Опис:**
Визначити механізм перевірки здоров'я IoT-датчиків.
Якщо датчик не відповідає > 5 хвилин — тригер Threshold Alert (US-12).

**NFR / Constraint:**
- Енергоефективність: health check ≤ 1 раз на хвилину
- Offline: алерти генеруються локально на вузлі

**Acceptance Criteria:**
- [ ] Визначено інтервал polling (heartbeat)
- [ ] Описано механізм алертингу без Cloud
- [ ] Є fallback: якщо датчик мертвий — показати "Last Known Value"

**Залежності:** #TT-* (Threshold Alert)
```
</details>

#### 🔒 Security — Безпека

Шифрування, доступ, ізоляція.

<details>
<summary>📋 Приклад: Security</summary>

**Заголовок:** `OPS-04 EP-01 US-02 | Key storage for Web of Trust on Android`

```
**Тип:** Ops Task
**Сквад:** Gamma
**Пов'язані Stories:** US-02
**Labels:** ops, squad:gamma, EP-01
**Категорія:** Security

**Опис:**
Визначити стратегію зберігання криптографічних ключів
Web of Trust на Android-пристрої. Ключі використовуються
для підпису транзакцій та верифікації довіри.

**NFR / Constraint:**
- Безпека: end-to-end шифрування, ключі не залишають пристрій
- Hardware: Android 8+ (Android Keystore System)

**Acceptance Criteria:**
- [ ] Обрано сховище ключів (Android Keystore / EncryptedSharedPreferences)
- [ ] Ключі не експортуються з пристрою
- [ ] При factory reset — ключі знищуються, вузол потребує перереєстрації
- [ ] Backup ключів — заборонено (single point of identity)

**Залежності:** #DR-* (Trust Level), #TT-* (Mesh Identity)
```
</details>

---

## NFR Checklist

Для кожного компонента від Dev перевірте ці нефункціональні вимоги:

| NFR | Питання | Релевантний Constraint |
| :--- | :--- | :--- |
| **Offline** | Чи працює без Mesh / інтернету? | Offline-first |
| **Bandwidth** | Чи вкладається в 250 Kbps? | Mesh Transport |
| **Memory** | Чи вкладається в 1 GB RAM? | Hardware |
| **Battery** | Чи фонові процеси ≤ 5% / год? | Енергоефективність |
| **Encryption** | Чи всі дані зашифровані end-to-end? | Безпека |
| **Latency** | Прийнятний час відповіді? | (визначити) |
| **Data Loss** | Що відбувається при аварійному вимкненні? | Offline-first |

---

## Чеклист перед завершенням

- [ ] Вивчено Constraints з Product Backlog
- [ ] Переглянуто Technical Tasks від Dev
- [ ] Створено ≥ 3 тікети `ops` у GitHub Project
- [ ] Кожен тікет має категорію (Environment / CI-CD / Monitoring / NFR / Security)
- [ ] Для кожного компонента пройдено NFR Checklist
- [ ] Всі тікети мають залежності та лейбли скваду

---

**[⬅️ Повернутися до плану воркшопу](plan.md)** | **[⬅️ Повернутися до головного меню курсу](../index.md)**
