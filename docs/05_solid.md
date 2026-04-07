# Лекція 5: Принципи SOLID — Основа якісного дизайну

## ⚡ Експрес-опитування: Активація знань

На попередніх заняттях ми вже стикалися з "хорошим" та "поганим" дизайном. Давайте пригадаємо:

1.  **Інкапсуляція:** Ми ховали поля класу за `private` модифікатором. Яку проблему це вирішувало?
2.  **Інтерфейси:** Ми обговорювали інтерфейси (`List`, `Set`). Яку гнучкість це нам давало у порівнянні з використанням конкретних класів (як `ArrayList`)?
3.  **Фінальний проєкт:** Наша мета — спроєктувати гру. Як ви думаєте, що станеться, якщо всю логіку гри (рух, правила, штучний інтелект, малювання на екрані) написати в одному величезному класі `Game`?

<details markdown="1">
<summary>Наші очікування (відповідь)</summary>

1.  **Інкапсуляція** захищала дані від неконтрольованих змін ззовні. Це дозволяло нам змінювати *внутрішню* реалізацію класу, не ламаючи код, який його використовує.
2.  **Інтерфейси** дозволяли нам писати код, який працює з будь-якою реалізацією (напр., `List list = new ArrayList()` або `List list = new LinkedList()`). Код ставав гнучкішим.
3.  Якщо все написати в одному класі `Game`, він перетвориться на "монстра". Зміна логіки AI могла б зламати малювання. Додати нового гравця було б неможливо, не переписавши половину файлу. Такий код **крихкий** та **неможливо підтримувати**.

</details>

Сьогодні ми формалізуємо ці ідеї. **SOLID** — це акронім для п'яти фундаментальних принципів об'єктно-орієнтованого дизайну, сформульованих Робертом Мартіном. Ці принципи є дороговказом для створення зрозумілих, гнучких та легких у підтримці програмних систем. Дотримання SOLID допомагає уникнути "крихкого" коду, який ламається від найменших змін.

Давайте розглянемо кожен принцип детально з прикладами коду.

-----

## S — Single Responsibility Principle (Принцип єдиного обов'язку)

> 💡 **Ідея:** Клас повинен мати лише одну причину для змін.

Це означає, що кожен клас має виконувати лише **один, чітко визначений обов'язок**. Якщо клас робить занадто багато, його стає складно підтримувати.

### Приклад коду: Порушення SRP

Розглянемо клас, що обробляє замовлення.

```java
// ПОГАНИЙ ПРИКЛАД
public class OrderProcessor {

    public void process(Order order) {
        if (order.isValid() && save(order)) {
            sendConfirmationEmail(order);
        }
    }

    private boolean save(Order order) {
        MySqlConnection connection = new MySqlConnection("database.url");
        // Логіка збереження замовлення в базу даних MySQL...
        System.out.println("Збереження замовлення в БД.");
        return true;
    }

    private void sendConfirmationEmail(Order order) {
        String name = order.getCustomerName();
        String email = order.getCustomerEmail();
        // Логіка відправки листа клієнту...
        System.out.println("Відправка листа-підтвердження.");
    }
}
```



**Інтерпретація:** Цей клас `OrderProcessor` порушує SRP, оскільки він має **три причини для змін**:

1.  Зміна логіки обробки замовлення (метод `process`).
2.  Зміна способу збереження в базу даних (метод `save`).
3.  Зміна формату або способу відправки листа (метод `sendConfirmationEmail`).

### Приклад коду: Дотримання SRP

Розділимо обов'язки на три окремі класи.

```java
// КЛАС 1: ВІДПОВІДАЄ ТІЛЬКИ ЗА ЗБЕРЕЖЕННЯ
public class MySQLOrderRepository {
    public boolean save(Order order) {
        MySqlConnection connection = new MySqlConnection("database.url");
        // Логіка збереження...
        System.out.println("Збереження замовлення в БД.");
        return true;
    }
}

// КЛАС 2: ВІДПОВІДАЄ ТІЛЬКИ ЗА ВІДПРАВКУ ЛИСТІВ
public class ConfirmationEmailSender {
    public void sendConfirmationEmail(Order order) {
        String name = order.getCustomerName();
        String email = order.getCustomerEmail();
        // Логіка відправки...
        System.out.println("Відправка листа-підтвердження.");
    }
}

// КЛАС 3: ВІДПОВІДАЄ ТІЛЬКИ ЗА КООРДИНАЦІЮ ПРОЦЕСУ
public class OrderProcessor {
    public void process(Order order) {
        MySQLOrderRepository repository = new MySQLOrderRepository();
        ConfirmationEmailSender mailSender = new ConfirmationEmailSender();

        if (order.isValid() && repository.save(order)) {
            mailSender.sendConfirmationEmail(order);
        }
    }
}
```



**Інтерпретація:** Тепер кожен клас має лише один обов'язок. Якщо нам потрібно змінити логіку роботи з БД, ми змінюємо лише `MySQLOrderRepository`. `OrderProcessor` залишається незмінним.

-----

## O — Open/Closed Principle (Принцип відкритості/закритості)

> 💡 **Ідея:** Програмні сутності (класи, модулі) мають бути відкритими для розширення, але закритими для модифікації.

Це означає, що ви повинні мати можливість додавати нову функціональність, **не змінюючи існуючий, вже протестований код**. Зазвичай це досягається за допомогою наслідування або інтерфейсів.

### Приклад коду: Дотримання OCP

Уявімо, що нам потрібно додати якісь дії до та після обробки замовлення, не змінюючи сам `OrderProcessor`.

```java
public class OrderProcessorWithPreAndPostProcessing extends OrderProcessor {

    @Override
    public void process(Order order) {
        beforeProcessing();
        super.process(order); // Викликаємо оригінальну логіку
        afterProcessing();
    }

    private void beforeProcessing() {
        // Виконуємо дії до обробки
        System.out.println("Виконуємо попередню обробку...");
    }

    private void afterProcessing() {
        // Виконуємо дії після обробки
        System.out.println("Виконуємо фінальні дії...");
    }
}
```



**Інтерпретація:** Ми **розширили** поведінку `OrderProcessor`, створивши новий клас, але **не модифікували** оригінальний. Оригінальний клас `OrderProcessor` залишився "закритим" для змін, що є безпечним.

-----

## L — Liskov Substitution Principle (Принцип підстановки Барбари Лісков)

> 💡 **Ідея:** Об'єкти в програмі повинні бути замінними на екземпляри їхніх підтипів без зміни коректності виконання програми.

Простими словами: якщо у вас є об'єкт батьківського класу, ви повинні мати можливість замінити його на об'єкт будь-якого з його класів-нащадків, і програма не повинна зламатися. Нащадок не повинен змінювати "контракт" поведінки батька .

### Приклад коду: Порушення LSP

Батьківський клас перевіряє, чи є товар на складі, і повертає `true` або `false`.

```java
// БАТЬКІВСЬКИЙ КЛАС
public class OrderStockValidator {
    public boolean isValid(Order order) {
        for (Item item : order.getItems()) {
            if (!item.isInStock()) {
                return false; // Повертає false, якщо товару немає
            }
        }
        return true;
    }
}
```



Клас-нащадок додає перевірку, чи товар запаковано, але при цьому змінює поведінку: замість того, щоб просто повернути `false`, він кидає виняток.

```java
// ПОГАНИЙ ПРИКЛАД НАЩАДКА
public class OrderStockAndPackValidator extends OrderStockValidator {
    @Override
    public boolean isValid(Order order) {
        for (Item item : order.getItems()) {
            if (!item.isInStock() || !item.isPacked()) {
                // ПОРУШЕННЯ: Батьківський клас ніколи не кидав виняток.
                // Клієнтський код, що очікує boolean, зламається.
                throw new IllegalStateException(
                    String.format("Order %d is not valid!", order.getId())
                );
            }
        }
        return true;
    }
}
```



**Інтерпретація:** `OrderStockAndPackValidator` не можна безпечно підставити замість `OrderStockValidator`, оскільки він порушує контракт (починає кидати винятки, яких клієнт не очікує).

-----

## I — Interface Segregation Principle (Принцип розділення інтерфейсу)

> 💡 **Ідея:** Краще мати багато спеціалізованих ("маленьких") інтерфейсів, ніж один загального призначення ("великий").

Клієнти не повинні залежати від методів, які вони не використовують.

### Приклад коду (доповнення)

**Поганий приклад:** Створюємо один "великий" інтерфейс для працівника.

```java
// "ТОВСТИЙ" ІНТЕРФЕЙС
interface IWorker {
    void work();
    void eat();
}

class HumanWorker implements IWorker {
    public void work() { /* ... */ }
    public void eat() { /* ... */ }
}

class RobotWorker implements IWorker {
    public void work() { /* ... */ }
    public void eat() {
        // Робот не їсть. Ми змушені реалізувати пустий метод. Це порушення ISP.
    }
}
```

**Хороший приклад:** Розділяємо інтерфейс на два менших.

```java
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

class HumanWorker implements Workable, Eatable {
    public void work() { /* ... */ }
    public void eat() { /* ... */ }
}

class RobotWorker implements Workable { // Робот реалізує тільки те, що йому потрібно
    public void work() { /* ... */ }
}
```

**Інтерпретація:** Тепер класи реалізують лише ті інтерфейси (контракти), які їм дійсно потрібні.

-----

## D — Dependency Inversion Principle (Принцип інверсії залежностей)

> 💡 **Ідея:** Модулі вищого рівня не повинні залежати від модулів нижчого рівня. Обидва повинні залежати від абстракцій (інтерфейсів).

Простими словами: ваш код має залежати від "контрактів" (інтерфейсів), а не від конкретних "виконавців" (класів) .

### Приклад коду (доповнення на основі SRP)

**Поганий приклад:** `OrderProcessor` напряму створює і залежить від **конкретного** класу `MySQLOrderRepository`.

```java
// OrderProcessor залежить від конкретної реалізації
public class OrderProcessor {
    public void process(Order order) {
        MySQLOrderRepository repository = new MySQLOrderRepository(); // <-- Жорстка залежність
        repository.save(order);
        // ...
    }
}
```

**Інтерпретація:** Щоб змінити базу даних на PostgreSQL, нам доведеться **змінювати код** `OrderProcessor`, що порушує OCP.

**Хороший приклад:** Створюємо абстракцію і "впроваджуємо" залежність.

```java
// 1. Створюємо АБСТРАКЦІЮ (інтерфейс)
interface OrderRepository {
    boolean save(Order order);
}

// 2. Конкретна реалізація імплементує інтерфейс
public class MySQLOrderRepository implements OrderRepository {
    public boolean save(Order order) { /* ... */ }
}

// 3. Високорівневий модуль залежить від АБСТРАКЦІЇ
public class OrderProcessor {
    private final OrderRepository repository;

    // Залежність "впроваджується" через конструктор (Dependency Injection)
    public OrderProcessor(OrderRepository repository) {
        this.repository = repository;
    }

    public void process(Order order) {
        repository.save(order);
        // ...
    }
}

// Десь у головному методі програми:
OrderRepository mySqlRepo = new MySQLOrderRepository();
OrderProcessor processor = new OrderProcessor(mySqlRepo); // Впроваджуємо залежність
```

**Інтерпретація:** Тепер `OrderProcessor` нічого не знає про MySQL. Він залежить лише від "контракту" `OrderRepository`. Ми можемо легко підставити `PostgresOrderRepository` або `MongoOrderRepository`, не змінюючи `OrderProcessor`.

-----

## Практичне застосування

Теорія SOLID — це основа. Тепер подивімось, як вона працює в реальному житті:
[Практикум 5: Від спагеті до SOLID](p05_solid.md): Ми візьмемо "спагеті-код" і проведемо повний рефакторинг, застосовуючи всі п'ять принципів.`
[Практикум 6: Модульний дизайн (DIP, IoC, DI)](p06_module_design.md): Ми глибоко зануримось у принцип інверсії залежностей і навчимося писати по-справжньому гнучкий код. `

## Екзаменаційний пул (Exam Questions)

1.  **SRP.** Подивіться на "поганий" приклад для `OrderProcessor`. Назвіть три причини, через які цей клас може потребувати змін.
2.  **OCP.** Як наслідування дозволяє дотримуватися принципу відкритості/закритості? Наведіть приклад з лекції.
3.  **LSP.** Чому клас `OrderStockAndPackValidator`, який кидає виняток, порушує принцип підстановки Лісков?
4.  **ISP.** Уявіть інтерфейс `Bird` з методами `fly()` та `tweet()`. Чому клас `Penguin` буде порушувати принцип розділення інтерфейсу, якщо реалізує цей інтерфейс? Як це виправити?
5.  **DIP.** Поясніть своїми словами, чому в "хорошому" прикладі для DIP ми можемо легко замінити `MySQLOrderRepository` на `PostgresOrderRepository`, не змінюючи клас `OrderProcessor`.

<details markdown="1">
<summary>Еталонна відповідь</summary>

1.  **SRP:** Клас `OrderProcessor` має три причини для змін:

    1.  Зміна бізнес-логіки обробки замовлення (метод `process`).
    2.  Зміна способу збереження в базу даних (метод `save`, наприклад, перехід з MySQL на PostgreSQL).
    3.  Зміна способу відправки сповіщень (метод `sendConfirmationEmail`, наприклад, перехід з Email на SMS).

2.  **OCP:** Наслідування дозволяє **розширити** поведінку, не **змінюючи** оригінальний код. У лекції ми створили `OrderProcessorWithPreAndPostProcessing`, який успадкував `OrderProcessor`. Ми додали нову логіку (`beforeProcessing` та `afterProcessing`), але при цьому не торкнулися жодного рядка в оригінальному, вже протестованому класі `OrderProcessor`.

3.  **LSP:** Батьківський клас `OrderStockValidator` має "контракт": метод `isValid` *завжди* повертає `boolean` (`true` або `false`). Клієнтський код, який його викликає, очікує саме такої поведінки. Нащадок `OrderStockAndPackValidator` порушує цей контракт, оскільки в деяких випадках він не повертає `boolean`, а кидає `IllegalStateException`. Якщо клієнтський код не очікує цього винятку (а він не очікує, бо працює з батьківським типом), програма "впаде".

4.  **ISP:** Клас `Penguin` порушить ISP, тому що він буде змушений реалізувати метод `fly()`, який для нього не має сенсу (пінгвіни не літають). Йому доведеться або залишати метод пустим, або кидати виняток.

      * **Як виправити:** Розділити "товстий" інтерфейс `Bird` на два менших, спеціалізованих інтерфейси: `Flyable` (з методом `fly()`) та `Tweetable` (з методом `tweet()`). Тоді клас `Sparrow` (горобець) реалізує обидва, а `Penguin` — лише `Tweetable` (якщо вони цвірінькають) або взагалі жодного з них, якщо це недоречно.

5.  **DIP:** Клас `OrderProcessor` більше не знає про існування *конкретного* класу `MySQLOrderRepository`. Він знає лише про *абстракцію* — інтерфейс `OrderRepository`. Оскільки і `MySQLOrderRepository`, і `PostgresOrderRepository` реалізують один і той самий інтерфейс (контракт), ми можемо "підсунути" `OrderProcessor`-у будь-який з них (через конструктор), і він продовжить працювати, навіть не помітивши заміни.

</details>