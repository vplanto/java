# Практикум 4: Симулятор "Банки" Monobank

## Вступ

**Мета практикуму:** Створити спрощену консольну програму, що імітує функціональність "Банки" для збору коштів, як у Monobank. Ми пройдемо повний цикл: від створення проекту до написання коду та тестів.

**Навіщо ця задача?** Вона ідеально ілюструє, як на практиці використовуються основні типи колекцій для вирішення різних завдань:
* **`List`** — для зберігання повної історії операцій.
* **`Map`** — для агрегації та підрахунку даних (хто скільки задонатив).
* **`Set`** — для відстеження унікальних учасників(Детально про те, чому ми обираємо саме ці інтерфейси, ми говорили у [Лекції 3: Як обрати правильну колекцію](03_choosing_right_collection.md)).

---

## Крок 1: Створення Maven-проєкту

Спочатку створимо наш проєкт за допомогою Maven. Це забезпечить правильну структуру та дозволить легко підключити бібліотеку для тестування.

1.  **Створіть новий Maven-проєкт** у вашій IDE (IntelliJ IDEA).
2.  **Налаштуйте `pom.xml`:** Відкрийте файл `pom.xml` і переконайтеся, що у вас вказана версія Java 17 або вище, а також додайте залежність для тестування (JUnit 5).

    ```xml
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    ```
3.  Перевірте, що Maven створив правильну структуру: `src/main/java` та `src/test/java`.

---

## Крок 2: Моделювання даних

Нам потрібен спосіб представити один окремий донат. Для таких простих структур-переносників даних у сучасній Java ідеально підходять **`records`**.

-----

## Що таке Java `record`? (Коротко)

По суті, `record` — це **синтаксичний цукор** для створення простих, **незмінних (immutable*)** класів-контейнерів для даних.

**Раніше**, щоб створити клас для зберігання, наприклад, двох полів, потрібно було вручну писати конструктор, гетери для кожного поля, а також перевизначати методи `equals()`, `hashCode()` та `toString()`. Це десятки рядків коду.

**Зараз**, з `record`, ви просто описуєте дані, які хочете зберігати:

```java
public record Donation(String donatorName, double amount) {}
```

І все. Цей **один рядок** змушує компілятор автоматично згенерувати для вас:

  * Приватні `final` поля (`donatorName` та `amount`).
  * Публічний конструктор для всіх полів.
  * Публічні методи для отримання значень (`donatorName()` та `amount()`).
  * Правильні реалізації `equals()`, `hashCode()` та `toString()`.

**Коли використовувати `record`?**
Завжди, коли вам потрібен простий клас для передачі незмінних даних, наприклад:

  * DTO (Data Transfer Objects).
  * Результати запитів з бази даних.
  * Прості сутності, як `Donation` у нашому практикумі.

-----

**Що таке "незмінний" (immutable)?**

Уявіть, що ви написали повідомлення **ручкою на папері**. Ви не можете його змінити чи стерти. Щоб щось виправити, вам потрібно взяти **новий аркуш паперу** і написати повідомлення заново. Це і є **незмінний (immutable)** об'єкт.

 Звичайний (змінний) об'єкт — це як повідомлення, написане **на дошці маркером**. Ви можете в будь-який момент стерти частину і написати щось нове, змінюючи оригінал.

Об'єкт `record`, як і повідомлення на папері, не можна змінити після створення.

---

1.  У теці `src/main/java` створіть пакет (наприклад, `com.example.jar`).
2.  У цьому пакеті створіть новий `record` з назвою `Donation`.

    ```java
    // src/main/java/com/example/jar/Donation.java
    public record Donation(String donatorName, double amount) {
    }
    ```
    Цей короткий запис автоматично створює клас з приватними фінальними полями `donatorName` та `amount`, конструктором, гетерами, а також методами `equals()`, `hashCode()` та `toString()`.

---

## Крок 3: Створення класу `Jar`

Це буде наш основний клас, що містить всю логіку "Банки".

1.  У тому ж пакеті створіть клас `Jar`.
2.  Додайте поля, пояснюючи вибір кожної колекції:

    ```java
    // src/main/java/com/example/jar/Jar.java
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.List;
    import java.util.Map;
    import java.util.Set;

    public class Jar {
        private final double targetAmount; // Цільова сума збору
        private final List<Donation> donations; // Для зберігання ВСІХ донатів по порядку
        private final Map<String, Double> donatorTotals; // Для підрахунку загальної суми від кожної людини
        private final Set<String> uniqueDonators; // Для зберігання унікальних імен донатерів

        public Jar(double targetAmount) {
            this.targetAmount = targetAmount;
            this.donations = new ArrayList<>();
            this.donatorTotals = new HashMap<>();
            this.uniqueDonators = new HashSet<>();
        }
    }
    ```
3.  Тепер додамо основний метод — `addDonation`:

    ```java
    // ... всередині класу Jar ...
    public void addDonation(String name, double amount) {
        // 1. Створюємо об'єкт донату
        Donation newDonation = new Donation(name, amount);

        // 2. Додаємо його до загальної історії (List)
        donations.add(newDonation);

        // 3. Додаємо ім'я до множини унікальних донатерів (Set)
        // Якщо таке ім'я вже є, нічого не зміниться
        uniqueDonators.add(name);

        // 4. Оновлюємо загальну суму для цієї людини (Map)
        double currentTotal = donatorTotals.getOrDefault(name, 0.0);
        donatorTotals.put(name, currentTotal + amount);
    }
    ```
4.  Додамо кілька методів для отримання інформації:
    ```java
    // ... всередині класу Jar ...
    public double getTotalAmount() {
        double total = 0;
        for (Donation donation : donations) {
            total += donation.amount();
        }
        return total;
    }

    public int getUniqueDonatorsCount() {
        return uniqueDonators.size();
    }

    public double getTargetAmount() {
        return this.targetAmount;
    }

    public List<Donation> getDonationHistory() {
        return new ArrayList<>(donations); // Повертаємо копію, щоб захистити оригінальний список
    }
    ```

---

## Крок 4: Пишемо тести (TDD-стиль)

Перш ніж запускати додаток, перевіримо, чи наша логіка працює коректно.
1.  У теці `src/test/java` створіть той самий пакет (`com.example.jar`) і клас `JarTest`.
2.  Напишіть кілька тестів:

    ```java
    // src/test/java/com/example/jar/JarTest.java
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.*;

    class JarTest {

        @Test
        void testAddSingleDonation() {
            Jar jar = new Jar(1000);
            jar.addDonation("Vitalii", 100);

            assertEquals(100, jar.getTotalAmount());
            assertEquals(1, jar.getUniqueDonatorsCount());
        }

        @Test
        void testAddMultipleDonationsFromSamePerson() {
            Jar jar = new Jar(1000);
            jar.addDonation("Olena", 50);
            jar.addDonation("Olena", 150);

            assertEquals(200, jar.getTotalAmount());
            assertEquals(1, jar.getUniqueDonatorsCount());
        }

        @Test
        void testUniqueDonatorsCount() {
            Jar jar = new Jar(1000);
            jar.addDonation("Andrii", 100);
            jar.addDonation("Bohdan", 200);
            jar.addDonation("Andrii", 50);

            assertEquals(350, jar.getTotalAmount());
            assertEquals(2, jar.getUniqueDonatorsCount());
        }
    }
    ```
3.  Запустіть команду `mvn test` у терміналі. Ви маєте побачити `BUILD SUCCESS`.

---

## Крок 5: Створення консольного інтерфейсу

Тепер, коли ми впевнені, що наш клас працює, створимо простий консольний додаток.
1.  Створіть клас `Main` з методом `main`.
2.  Додайте код, що імітує роботу з банкою:

    ```java
    // src/main/java/com/example/jar/Main.java
    public class Main {
        public static void main(String[] args) {
            Jar myJar = new Jar(10000); // Створюємо банку на 10 000

            // Імітуємо донати
            myJar.addDonation("Степан", 500);
            myJar.addDonation("Марія", 1000);
            myJar.addDonation("Степан", 250);
            myJar.addDonation("Ігор", 2000);

            // Виводимо статистику
            System.out.println("=== ЗВІТ ПО БАНЦІ ===");
            System.out.printf("Ціль: %.2f грн%n", myJar.getTargetAmount());
            System.out.printf("Зібрано: %.2f грн%n", myJar.getTotalAmount());
            System.out.println("Кількість унікальних донатерів: " + myJar.getUniqueDonatorsCount());
            System.out.println("\n--- Історія донатів ---");
            for (Donation donation : myJar.getDonationHistory()) {
                System.out.printf("%s задонатив(ла) %.2f грн%n", donation.donatorName(), donation.amount());
            }
        }
    }
    ```

---

## Контрольні питання

1.  **Вибір колекції.** Чому для зберігання історії донатів був обраний `List`, а для унікальних донатерів — `Set`? Що б змінилося (і які були б недоліки), якби ми використали `List` в обох випадках?
2.  **Ефективність.** Поясніть призначення `Map` у класі `Jar`. Яку проблему ми б отримали, якби нам довелося щоразу обчислювати загальну суму для кожного донатера, перебираючи `List<Donation>`?
3.  **Код.** У методі `addDonation` є рядок `donatorTotals.getOrDefault(name, 0.0)`. Що він робить і чому це краще, ніж просто `donatorTotals.get(name)`?
4.  **Розширення.** Як би ви змінили клас `Jar`, щоб додати метод `getTopDonators(int n)`, який повертає `List` імен `n` людей, що задонатили найбільше? (Достатньо описати логіку словами).