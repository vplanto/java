# Практикум 1: Zero to Hero. Створення REST-сервісу з нуля

**Тип:** Hands-on Lab
**Рівень:** Junior Strong
**Інструменти:** Java 17+, Maven, IntelliJ IDEA, Spring Boot 3.x.
**Мета:** Розгорнути промисловий веб-сервер за 10 хвилин. Зрозуміти різницю між «Java Console App» та «Web Container».

> [!NOTE]
> **Наскрізний домен: Книжкова Бібліотека** 📚
> Починаючи з цього практикуму і до **P08**, ви будуєте один проєкт — REST API для управління бібліотекою книг. Кожен наступний практикум додає новий шар: Service → DTO → Validation → Tests → Docker. Не видаляйте код з попередніх практикумів — ви нарощуєте його!

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
У Enterprise-розробці вартість налаштування проєкту вручну (створення папок, пошук `.jar` файлів, налаштування білд-скриптів) — це втрачені гроші бізнесу. Ми використовуємо **Spring Initializr** — генератор, який створює «скелет» (Scaffolding) аплікації з гарантовано сумісними версіями бібліотек.

### 1.1: Генерація через Spring Initializr
1.  Перейдіть на [start.spring.io](https://start.spring.io/).
2.  **Project Metadata** (Налаштування білда):
    * **Project:** Maven.
    * **Language:** Java.
    * **Spring Boot:** 3.x.x (остання стабільна).
    * **Group:** `ua.edu`
    * **Artifact:** `library-service`
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
Веб-сервер повинен мати точку входу (Endpoint). Створіть клас `BookController`, який обробляє HTTP-запити для роботи з книгами.

**Файл: src/main/java/ua/edu/libraryservice/controller/BookController.java**
```java
package ua.edu.libraryservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    // GET /api/books/ping — перевірка, що сервер живий
    @GetMapping("/ping")
    public String ping() {
        return "Library Service: OK";
    }
}
```

> [!TIP]
> **Анотації — це метадані**
> `@RestController` каже Spring: «Знайди цей клас під час старту, створи його екземпляр (Bean) і направляй сюди HTTP-запити».
> `@RequestMapping("/api/books")` задає базовий URL для всіх методів у цьому контролері.

---

## Частина 4: Запуск Embedded Server (5 хв)

### Завдання
1. Відкрийте клас `LibraryServiceApplication`.
2. Запустіть метод `main` (зелена стрілочка в IDE).
3. Перевірте консоль: ви маєте побачити рядок `Tomcat started on port 8080 (http)`.
4. Відкрийте браузер: `http://localhost:8080/api/books/ping`

> [!IMPORTANT]
> **Концептуальна зміна: Cloud Native**
> Ми не встановлювали веб-сервер (Tomcat) окремо. Spring Boot використовує **Embedded servlet container** (вбудований сервер). Це робить наш додаток автономним (stand-alone), що ідеально підходить для запуску в Docker контейнерах.

---

## Частина 5: Еволюція даних — від String до JSON (10 хв)

### Бізнес-сценарій
У попередньому кроці ми повернули клієнту простий рядок (`String`). Але сучасні фронтенд-фреймворки (Angular, React) та мобільні додатки очікують структуровані об'єкти у форматі JSON. Як змусити Java-об'єкт автоматично перетворитися на JSON без ручного парсингу рядків?

### 5.1: Створення DTO (Data Transfer Object)
Ми використовуємо `record` для незмінних даних, що передаються по мережі.

**Завдання:** Створіть файл DTO для книги.

**Файл: src/main/java/ua/edu/libraryservice/dto/BookResponse.java**
```java
package ua.edu.libraryservice.dto;

public record BookResponse(Long id, String title, String author) {}
```

### 5.2: Оновлення Контролера
Тепер додамо метод, що повертає список книг у форматі JSON.

**Файл: BookController.java** — додайте новий метод:
```java
import ua.edu.libraryservice.dto.BookResponse;
import java.util.List;

    @GetMapping
    public List<BookResponse> getAllBooks() {
        // Поки що — статичні дані. У P04 ми перенесемо це в Service.
        return List.of(
            new BookResponse(1L, "Clean Code", "Robert C. Martin"),
            new BookResponse(2L, "The Pragmatic Programmer", "David Thomas")
        );
    }
```

> [!NOTE]
> **Магія серіалізації (Jackson)**
> Вам не потрібно вручну конвертувати об'єкти в JSON. Spring (за допомогою бібліотеки Jackson) автоматично серіалізує `List<BookResponse>` у JSON-масив:
> ```json
> [{"id":1,"title":"Clean Code","author":"Robert C. Martin"}, ...]
> ```

> [!TIP]
> **Технічний борг у коді вище**
> Ви бачите коментар «Поки що — статичні дані»? Це усвідомлений тимчасовий компроміс. У **P04** ми винесемо цю логіку в `BookService` — правильне місце для бізнес-логіки.

---

##  Контрольні питання
Перевірка засвоєння матеріалу (Definition of Done).

1. **Deployment:** Якщо ви скомпілюєте цей проєкт (`mvn package`), що ви отримаєте на виході і як це запустити на «чистому» Linux сервері?
2. **Annotations:** У чому різниця між `@Controller` та `@RestController`? Що станеться, якщо використати перший варіант у нашому прикладі?
3. **Networking:** Ви запустили додаток, але браузер каже «Connection Refused». При цьому в логах `Tomcat started on port 8080`. Назвіть дві ймовірні причини.

<details markdown="1">
<summary>Відповіді</summary>

1. **Fat JAR.** Ви отримаєте один `.jar` файл, який містить і ваш код, і всі бібліотеки, і сервер Tomcat. Запуск: `java -jar library-service.jar`. Жодних попередніх інсталяцій сервера не потрібно.

2. **View Resolution.** `@Controller` намагається знайти HTML-шаблон (View) з іменем, яке ви повернули. `@RestController` — це комбінація `@Controller` + `@ResponseBody`, він пише дані напряму у HTTP Body.

3. **Причини:**
    * Конфлікт портів (порт 8080 зайнятий іншим процесом, хоча тоді б додаток впав на старті).
    * Firewall блокує підключення.
    * Ви звертаєтесь по HTTPS, а сервер піднявся на HTTP.

</details>

---

**[⬅️ P02: Web архітектура](p02_spring_web_arch.md)** | **[P04: DI та архітектура ➡️](p04_spring_architecture_di.md)**

**[⬅️ Повернутися до головного меню курсу](../../index.md)**
