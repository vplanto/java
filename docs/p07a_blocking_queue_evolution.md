# Практикум 7a: Еволюція потокобезпечної черги (від `synchronized` до `BlockingQueue`)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](DISCLAIMER.md).

## Експрес-опитування

1. Чи відпускає потік монітор `synchronized`, коли викликає `Thread.sleep(1000)`?
2. У чому різниця між `notify()` та `notifyAll()` при роботі з одним монітором для кількох умов очікування?
3. Чому виклик `wait()` або `Condition.await()` завжди має стояти всередині циклу `while`, а не умови `if`?

<details markdown="1">
<summary>Відповіді для самоперевірки</summary>

1. **Ні.** `Thread.sleep()` призупиняє потік, але утримує всі захоплені монітори. Інші потоки не можуть зайти в синхронізовані методи того самого об'єкта.
2. `notify()` будить один довільний потік зі списку очікування монітора. Якщо на одному моніторі чекають і виробники (producers), і споживачі (consumers), `notify()` може розбудити потік того ж типу, що призведе до deadlock.
3. Через **spurious wakeups** (хибні пробудження на рівні ОС/JVM) та зміну стану іншим потоком між моментом сигналу й повторним захопленням лока. Цикл `while` гарантує повторну перевірку інваріанта.

</details>

## Постановка задачі

У [Практикумі 7](p07_race_condition.md) ми усунули race condition на одному лічильнику. Тепер розглянемо класичну задачу **Producer-Consumer**: обмежену чергу місткістю 10 елементів із трьома парами операцій:

- **Неблокуючі:** `offer(e)` (повертає `false`, якщо черга повна) та `poll()` (повертає `null`, якщо черга порожня).
- **Блокуючі:** `put(e)` (чекає, доки з'явиться місце) та `take()` (чекає, доки з'явиться елемент).
- **З таймаутом:** `offer(e, timeout)` та `poll(timeout)`.

Нижче наведено 8 послідовних ітерацій коду - від наївної реалізації до стандартного `ArrayBlockingQueue`. На кожному кроці спочатку знайдіть дефект самостійно, а потім перевіряйте розбір.

---

## Крок 1. Наївна реалізація (`T01_naive`)

```java
public class T01_naive {
    LinkedList<Something> queue = new LinkedList<>();

    public boolean offer(Something e) {
        if (queue.size() >= 10)
            return false;
        return queue.add(e);
    }

    public Something poll() {
        if (queue.isEmpty())
            return null;
        return queue.remove(0);
    }
}
```

**Питання:** які проблеми виникнуть при роботі кількох потоків?

<details markdown="1">
<summary>Розбір дефектів</summary>

1. **Check-then-act race condition:** між перевіркою `queue.size() >= 10` і `queue.add(e)` (або `isEmpty()` і `remove(0)`) інший потік може змінити чергу. Розмір перевищить 10 або `remove(0)` кине `IndexOutOfBoundsException`.
2. **Непотокобезпечна структура даних:** `LinkedList` не синхронізований; одночасна модифікація посилань вузлів ламає внутрішній стан списку та видимість змін між ядрами CPU.

</details>

---

## Крок 2. Базова синхронізація (`T02_synchro`)

```java
public class T02_synchro {
    LinkedList<Something> queue = new LinkedList<>();

    public synchronized boolean offer(Something e) {
        if (queue.size() >= 10)
            return false;
        return queue.add(e);
    }

    public synchronized Something poll() {
        if (queue.isEmpty())
            return null;
        return queue.remove(0);
    }
}
```

Тепер `offer` і `poll` потокобезпечні, але реалізують лише неблокуючий контракт. Додамо блокуючі `put` і `take`.

---

## Крок 3. Блокування через `Thread.sleep` (`T03_synchro_sleep`)

```java
public class T03_synchro_sleep {
    LinkedList<Something> queue = new LinkedList<>();

    public synchronized boolean offer(Something e) {
        if (queue.size() >= 10)
            return false;
        return queue.add(e);
    }

    public /*synchronized*/ void put(Something e) throws InterruptedException {
        while (!offer(e)) {
            Thread.sleep(1000);
        }
    }

    public synchronized Something poll() {
        if (queue.isEmpty())
            return null;
        return queue.remove(0);
    }

    public /*synchronized*/ Something take() throws InterruptedException {
        Something result;
        while ((result = poll()) == null) {
            Thread.sleep(1000);
        }
        return result;
    }
}
```

**Питання:**
1. Що станеться, якщо розкоментувати `synchronized` на `put` і `take`?
2. Які проблеми залишаються навіть без `synchronized` на `put`/`take`?

<details markdown="1">
<summary>Розбір дефектів</summary>

1. **Deadlock при розкоментуванні `synchronized`:** якщо черга порожня, потік у `take()` захопить монітор `this`, викличе `Thread.sleep(1000)` і **не відпустить монітор**. Жоден producer не зможе зайти в `offer()`, щоб додати елемент. Програма зависне назавжди.
2. **Недоліки polling через `sleep`:** висока затримка реакції (до 1 секунди навіть коли черга вже звільнилася), марне прокидання потоків, відсутність справедливості черги.

</details>

---

## Крок 4. Очікування на моніторі через `wait` / `notifyAll` (`T04_synchro_wait`)

```java
public class T04_synchro_wait {
    LinkedList<Something> queue = new LinkedList<>();

    public synchronized boolean offer(Something e) {
        if (queue.size() >= 10)
            return false;
        notifyAll();
        return queue.add(e);
    }

    public synchronized void put(Something e) throws InterruptedException {
        while (!offer(e)) {
            wait();
        }
    }

    public synchronized Something poll() {
        if (queue.isEmpty())
            return null;
        notifyAll();
        return queue.remove(0);
    }

    public synchronized Something take() throws InterruptedException {
        Something result;
        while ((result = poll()) == null) {
            wait();
        }
        return result;
    }
}
```

**Питання:** чи можна замінити `notifyAll()` на `notify()` у цій реалізації?

<details markdown="1">
<summary>Розбір</summary>

**Ні.** Усі потоки (і producers, і consumers) чекають на **одному** моніторі `this`. Якщо черга порожня, а в очікуванні стоять кілька consumers і producers, один consumer після забирання останнього елемента може через `notify()` розбудити іншого consumer замість producer. Розбуджений consumer знову засне, а сигнал буде втрачено (ризик deadlock).

Водночас `notifyAll()` створює зайву конкуренцію ("thundering herd"): прокидаються всі потоки, хоча продовжити роботу може лише один.

</details>

---

## Крок 5. Перехід на `ReentrantLock` і один `Condition` (`T05_locks`)

```java
public class T05_locks {
    LinkedList<Something> queue = new LinkedList<>();
    ReentrantLock lock = new ReentrantLock();
    Condition cond = lock.newCondition();

    public boolean offer(Something e) {
        lock.lock();
        try {
            if (queue.size() >= 10)
                return false;
            cond.signalAll();
            return queue.add(e);
        } finally {
            lock.unlock();
        }
    }

    public void put(Something e) throws InterruptedException {
        lock.lock();
        try {
            while (!offer(e)) {
                cond.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public Something poll() {
        lock.lock();
        try {
            if (queue.isEmpty())
                return null;
            cond.signalAll();
            return queue.remove(0);
        } finally {
            lock.unlock();
        }
    }

    public Something take() throws InterruptedException {
        lock.lock();
        try {
            Something result;
            while ((result = poll()) == null) {
                cond.await();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
```

Поведінка еквівалентна `T04_synchro_wait`: поки `Condition` один, викликати `cond.signal()` все ще небезпечно. Перевага `ReentrantLock` розкривається на наступному кроці.

---

## Крок 6. Два окремих `Condition`: `notEmpty` та `notFull` (`T06_locks_conditions`)

```java
public class T06_locks_conditions {
    LinkedList<Something> queue = new LinkedList<>();
    ReentrantLock lock = new ReentrantLock();
    Condition notEmpty = lock.newCondition();
    Condition notFull = lock.newCondition();

    public boolean offer(Something e) {
        lock.lock();
        try {
            if (queue.size() >= 10)
                return false;
            notEmpty.signal();
            return queue.add(e);
        } finally {
            lock.unlock();
        }
    }

    public void put(Something e) throws InterruptedException {
        lock.lock();
        try {
            while (!offer(e)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public Something poll() {
        lock.lock();
        try {
            if (queue.isEmpty())
                return null;
            notFull.signal();
            return queue.remove(0);
        } finally {
            lock.unlock();
        }
    }

    public Something take() throws InterruptedException {
        lock.lock();
        try {
            Something result;
            while ((result = poll()) == null) {
                notEmpty.await();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
```

Тепер producers чекають на `notFull`, а consumers - на `notEmpty`. Додавання елемента будить рівно одного consumer (`notEmpty.signal()`), а видалення - рівно одного producer (`notFull.signal()`).

---

## Крок 7. Операції з таймаутом (`T07_locks_timeouts`)

```java
// Примітка: після пробудження await() все одно чекає на повторне захоплення lock без урахування таймауту
public class T07_locks_timeouts {
    LinkedList<Something> queue = new LinkedList<>();
    ReentrantLock lock = new ReentrantLock();
    Condition notEmpty = lock.newCondition();
    Condition notFull = lock.newCondition();

    // offer(e), put(e), poll(), take() - аналогічно T06_locks_conditions

    public boolean offer(Something e, long timeout) throws InterruptedException {
        long deadLine = System.currentTimeMillis() + timeout;
        if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            try {
                for (;;) {
                    if (offer(e))
                        return true;
                    long now = System.currentTimeMillis();
                    if (now >= deadLine)
                        return false;
                    notFull.await(deadLine - now, TimeUnit.MILLISECONDS);
                }
            } finally {
                lock.unlock();
            }
        } else {
            return false;
        }
    }

    public Something poll(long timeout) throws InterruptedException {
        long deadLine = System.currentTimeMillis() + timeout;
        if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            try {
                for (;;) {
                    Something result = poll();
                    if (result != null)
                        return result;
                    long now = System.currentTimeMillis();
                    if (now >= deadLine)
                        return null;
                    notEmpty.await(deadLine - now, TimeUnit.MILLISECONDS);
                }
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }
}
```

Зверніть увагу на дві інженерні деталі:

- **Розрахунок `deadLine`:** при хибному пробудженні або перехопленні елемента іншим потоком залишок часу `deadLine - now` перераховується (у продакшн-коді для дедлайнів краще використовувати монотонний `System.nanoTime()`, а не `currentTimeMillis()`, який залежить від корекції системного годинника).
- **Межа гарантії таймауту:** навіть коли сплив час у `await(...)`, потік не поверне керування, доки повторно не захопить `lock`.

---

## Крок 8. Стандартна бібліотека (`T10_cbq`)

У промисловому коді не слід писати власну блокуючу чергу з нуля: стандартний пакет `java.util.concurrent` уже містить протестовані реалізації (`ArrayBlockingQueue`, `LinkedBlockingQueue`, `ConcurrentLinkedQueue`).

```java
public class T10_cbq {
    BlockingQueue<Something> queue = new ArrayBlockingQueue<>(10);

    public boolean offer(Something e) {
        return queue.offer(e);
    }

    public void put(Something e) throws InterruptedException {
        queue.put(e);
    }

    public boolean offer(Something e, long timeout) throws InterruptedException {
        return queue.offer(e, timeout, TimeUnit.MILLISECONDS);
    }

    public Something poll() {
        return queue.poll();
    }

    public Something take() throws InterruptedException {
        return queue.take();
    }

    public Something poll(long timeout) throws InterruptedException {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}
```

`ArrayBlockingQueue` усередині побудований саме за схемою Кроку 6-7 (масив фіксованої довжини, `ReentrantLock` та два `Condition`: `notEmpty` і `notFull`), але додатково обробляє всі граничні випадки та використовує `awaitNanos`.

---

## Стрес-стенд для перевірки (`TestConcurrency`)

Щоб побачити різницю між `T01_naive`..`T10_cbq` на практиці, запустіть їх під одночасним навантаженням 12+ потоків. `CyclicBarrier` гарантує одночасний старт усіх потоків, а `AtomicLongArray` збирає лічильники викликів та успішних операцій щосекунди протягом 10 секунд:

```java
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLongArray;

public class TestConcurrency implements Runnable {
    public static final int MIN_THREADS = 12;
    public static final Long DEF_TIMEOUT = 10L;

    public static void main(String[] args) {
        Object impl = new T10_cbq(); // підставте T01_naive .. T10_cbq
        new TestConcurrency(impl).run();
    }

    final Object impl;
    final List<Method> availableMethods = new ArrayList<>();

    public TestConcurrency(Object impl) {
        this.impl = impl;
        Class<?> implClass = impl.getClass();

        findMethod(availableMethods, implClass, "offer", Something.class);
        findMethod(availableMethods, implClass, "offer", Object.class);
        findMethod(availableMethods, implClass, "offer", Something.class, long.class);
        findMethod(availableMethods, implClass, "offer", Object.class, long.class, TimeUnit.class);
        findMethod(availableMethods, implClass, "put", Something.class);
        findMethod(availableMethods, implClass, "put", Object.class);
        findMethod(availableMethods, implClass, "poll");
        findMethod(availableMethods, implClass, "poll", long.class);
        findMethod(availableMethods, implClass, "poll", long.class, TimeUnit.class);
        findMethod(availableMethods, implClass, "take");

        if (availableMethods.isEmpty())
            throw new IllegalArgumentException("There is nothing to invoke in " + implClass);
    }

    private void findMethod(List<Method> collector, Class<?> clazz, String name, Class<?>... params) {
        try {
            Method m = clazz.getMethod(name, params);
            System.out.println("Implementation method found: " + m);
            collector.add(m);
        } catch (NoSuchMethodException ignored) {
        }
    }

    public void run() {
        List<Runner> threads = new ArrayList<>();
        while (threads.size() < MIN_THREADS) {
            for (Method m : availableMethods) {
                int idx = threads.size();
                Runner runner = new Runner(idx, m);
                System.out.println("Thread #" + idx + " invokes: " + getSignature(m));
                threads.add(runner);
            }
        }

        int len = threads.size();
        AtomicLongArray invStats = new AtomicLongArray(len);
        AtomicLongArray sucStats = new AtomicLongArray(len);
        CyclicBarrier barr = new CyclicBarrier(len + 1);
        for (int i = 0; i < len; i++) {
            threads.get(i).start(invStats, sucStats, barr);
        }

        try {
            barr.await();
            long startTime = System.currentTimeMillis();
            for (;;) {
                Thread.sleep(1000);
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("----- " + new Date()
                        + " ----- Time passed: " + elapsed + " -----"
                        + "\nInvocation counts: " + invStats
                        + "\nSuccess counts:    " + sucStats);
                if (elapsed >= 10_000)
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSignature(Method m) {
        StringBuilder sgn = new StringBuilder();
        sgn.append(m.getName()).append('(');
        Class<?>[] ptypes = m.getParameterTypes();
        for (int i = 0; i < ptypes.length; i++) {
            if (i > 0) sgn.append(", ");
            sgn.append(ptypes[i].getSimpleName());
        }
        sgn.append(')');
        return sgn.toString();
    }

    class Runner extends Thread {
        private final int idx;
        private final Method m;
        private final Class<?>[] paramTypes;
        private final Object[] params;
        private AtomicLongArray invStats;
        private AtomicLongArray sucStats;
        private CyclicBarrier barr;

        Runner(int idx, Method m) {
            this.idx = idx;
            this.m = m;
            this.paramTypes = m.getParameterTypes();
            this.params = new Object[paramTypes.length];
            setDaemon(true);
        }

        public void start(AtomicLongArray invStats, AtomicLongArray sucStats, CyclicBarrier barr) {
            this.invStats = invStats;
            this.sucStats = sucStats;
            this.barr = barr;
            start();
        }

        public void run() {
            try {
                barr.await();
                for (;;) {
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == long.class) {
                            params[i] = DEF_TIMEOUT;
                        } else if (paramTypes[i] == Something.class || paramTypes[i] == Object.class) {
                            params[i] = new Something();
                        } else if (paramTypes[i] == TimeUnit.class) {
                            params[i] = TimeUnit.MILLISECONDS;
                        } else {
                            throw new InternalError("Unsupported argument type: " + paramTypes[i]);
                        }
                    }
                    Object res = m.invoke(impl, params);
                    invStats.incrementAndGet(idx);
                    if (m.getReturnType() == void.class || (res != null && !res.equals(Boolean.FALSE))) {
                        sucStats.incrementAndGet(idx);
                    }
                }
            } catch (Throwable e) {
                System.err.println("Exception in runner thread #" + idx);
                e.printStackTrace();
            }
        }
    }
}
```

## Контрольні питання

<details markdown="1">
<summary>Перевірити розуміння</summary>

1. **Чому `T04_synchro_wait` викликає `notifyAll()` до `queue.add(e)` і це все одно коректно?**
   Оскільки метод `synchronized`, потік ще утримує монітор `this`. Розбуджені потоки не зможуть зайти в критичну секцію, доки поточний потік не завершить `queue.add(e)` і не вийде з методу.
2. **Чому в `T07_locks_timeouts` не можна передавати незмінний `timeout` у кожну ітерацію `notFull.await(timeout, ...)`?**
   При повторних пробудженнях без вільного місця в черзі загальний час очікування зростав би необмежено замість дотримання заданого таймауту.
3. **Навіщо в `TestConcurrency` використано `CyclicBarrier(len + 1)`?**
   Щоб усі робочі потоки стартували одночасно після завершення ініціалізації, створюючи максимальну конкуренцію (contention) з першої мілісекунди.

</details>
