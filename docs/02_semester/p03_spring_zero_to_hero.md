# Практикум 1: Zero to Hero. Створення REST-сервісу з нуля

**Тип:** Hands-on Lab
**Рівень:** Junior Strong
**Інструменти:** Java 17+, Maven, IntelliJ IDEA, Spring Boot 3.x.
**Мета:** Розгорнути промисловий веб-сервер за 10 хвилин. Зрозуміти різницю між "Java Console App" та "Web Container".

---

##  Експрес-опитування: Вхідний поріг

Перевіримо ваше розуміння базових принципів HTTP та Java перед стартом.

1.  **Transport:** Чому ми використовуємо HTTP, а не пишемо на голих TCP-сокетах?
2.  **Packaging:** Що таке `.jar` файл фізично?
3.  **MIME-Types:** Як браузер розуміє, що сервер віддав йому саме JSON, а не просто текст, схожий на JSON?

<details markdown="1">
<summary>Відповіді (Самоперевірка)</summary>

1.  **Абстракція.** HTTP дає готовий набір дієслів (GET, POST) та заголовків. Писати на TCP — це як паяти мікросхему замість програмування. Дорого і довго.
2.  **ZIP-архів.** Це просто папка з скомпільованими `.class` файлами та метаданими, стиснута алгоритмом Zip.
3.  **Header `Content-Type`.** Сервер має додати заголовок `application/json`. Без нього браузер вважатиме це звичайним текстом (`text/plain`).

</details>

---

## Частина 1: Scaffolding та генерація проєкту (10 хв)

### Бізнес-сценарій
У Enterprise-розробці вартість налаштування проєкту вручну (створення папок, пошук `.jar` файлів, налаштування білд-скриптів) — це втрачені гроші бізнесу. Ми використовуємо **Spring Initializr** — генератор, який створює "скелет" (Scaffolding) аплікації з гарантовано сумісними версіями бібліотек.

### 1.1: Генерація через Spring Initializr
1.  Перейдіть на [start.spring.io](https://start.spring.io/).
2.  **Project Metadata** (Налаштування білда):
    * **Project:** Maven.
    * **Language:** Java.
    * **Spring Boot:** 3.x.x (остання стабільна).
    * **Group:** `ua.edu`
    * **Artifact:** `demo-service`
    * **Java:** 17 (або 21 LTS).
3.  **Dependencies** (Критично важливо):
    * Додайте **Spring Web**. Це ключовий стартер для розробки веб-застосунків або RESTful сервісів.
    * *(Опціонально)* **Lombok**.
4.  Натисніть **GENERATE**, розпакуйте та відкрийте файл `pom.xml` у вашій IDEA.

---

## Частина 2: Анатомія pom.xml та Dependency Management (5 хв)

Подивіться у файл `pom.xml`. Чому у `spring-boot-starter-web` немає версії?

**Файл: pom.xml**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

> [!NOTE]
> **Інженерний інсайт: BOM (Bill of Materials)**
> Версії керуються через батьківський `spring-boot-starter-parent`. Це вирішує проблему **Dependency Hell**: розробники Spring вже протестували, що Tomcat версії X працює з Jackson версії Y. Вам не потрібно підбирати їх вручну.

---

## Частина 3: Перший Endpoint (Controller) (10 хв)

### Завдання
Веб-сервер повинен мати точку входу (Endpoint). Створіть клас `GreetingController`, який буде обробляти HTTP-запити.

**Файл: src/main/java/ua/edu/demoservice/controller/GreetingController.java**
```java
package ua.edu.demoservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // Реєструє бін та вмикає серіалізацію відповіді в Body
public class GreetingController {

    // Мапимо GET запит на URL /api/hello
    @GetMapping("/api/hello")
    public String sayHello(@RequestParam(value = "name", defaultValue = "Student") String name) {
        // Tomcat загорне цей рядок у HTTP Response 200 OK
        return String.format("System status: OK. Welcome, %s!", name);
    }
}
```

> [!TIP]
> **Анотації — це метадані**
> `@RestController` каже Spring: "Знайди цей клас під час старту, створи його екземпляр (Bean) і направляй сюди HTTP-запити".

---

## Частина 4: Запуск Embedded Server (5 хв)

### Завдання
1. Відкрийте клас `DemoServiceApplication`.
2. Запустіть метод `main` (зелена стрілочка в IDE).
3. Перевірте консоль: ви маєте побачити рядок `Tomcat started on port 8080 (http)`.

> [!IMPORTANT]
> **Концептуальна зміна: Cloud Native**
> Ми не встановлювали веб-сервер (Tomcat) окремо. Spring Boot використовує **Embedded servlet container** (вбудований сервер). Це робить наш додаток автономним (stand-alone), що ідеально підходить для запуску в Docker контейнерах.

---

## Частина 5: Еволюція даних — від String до JSON (10 хв)

### Бізнес-сценарій
У попередньому кроці ми повернули клієнту простий рядок (`String`). Але сучасні фронтенд-фреймворки (Angular, React) та мобільні додатки очікують структуровані об'єкти у форматі JSON. Як змусити Java-об'єкт автоматично перетворитися на JSON без ручного парсингу рядків?

### 5.1: Створення DTO (Data Transfer Object)
Ми використовуємо `record` для незмінних даних, що передаються по мережі.

**Завдання:** Створіть файл DTO.

**Файл: src/main/java/ua/edu/demoservice/dto/SystemStatus.java**
```java
package ua.edu.demoservice.dto;

public record SystemStatus(String system, int users, boolean active) {}
```

### 5.2: Оновлення Контролера
Тепер змінимо наш контролер, щоб він повертав об'єкт замість рядка.

**Завдання:** Додайте новий метод у ваш `GreetingController`.

**Файл: src/main/java/ua/edu/demoservice/controller/GreetingController.java**
```java
    @GetMapping("/api/status")
    public SystemStatus getStatus() {
        return new SystemStatus("Demo-App", 42, true);
    }
```

> [!NOTE]
> **Магія серіалізації (Jackson)**
> Вам не потрібно вручну писати конкатенацію рядків. Spring (за допомогою бібліотеки Jackson) автоматично перехопить об'єкт `SystemStatus` і згенерує ідеальний JSON: `{"system": "Demo-App", "users": 42, "active": true}`.

---

##  Контрольні питання
Перевірка засвоєння матеріалу (Definition of Done).

1. **Deployment:** Якщо ви скомпілюєте цей проєкт (`mvn package`), що ви отримаєте на виході і як це запустити на "чистому" Linux сервері?
2. **Annotations:** У чому різниця між `@Controller` та `@RestController`? Що станеться, якщо використати перший варіант у нашому прикладі?
3. **Networking:** Ви запустили додаток, але браузер каже "Connection Refused". При цьому в логах `Tomcat started on port 8080`. Назвіть дві ймовірні причини.

<details markdown="1">
<summary>Відповіді</summary>

1. **Fat JAR.** Ви отримаєте один `.jar` файл, який містить і ваш код, і всі бібліотеки, і сервер Tomcat. Запуск: `java -jar app.jar`. Жодних попередніх інсталяцій сервера не потрібно.


2. **View Resolution.** `@Controller` намагається знайти HTML-шаблон (View) з іменем, яке ви повернули. `@RestController` — це комбінація `@Controller` + `@ResponseBody`, він пише дані напряму у HTTP Body.
3. **Причини:**
* Конфлікт портів (порт 8080 зайнятий іншим процесом, хоча тоді б додаток впав на старті).
* Firewall блокує підключення.
* Ви звертаєтесь по HTTPS, а сервер піднявся на HTTP.



</details>
---

**[⬅️ P02: Web архітектура](p02_spring_web_arch.md)** | **[P04: DI та архітектура ➡️](p04_spring_architecture_di.md)**

**[⬅️ Повернутися до головного меню курсу](index.md)**
