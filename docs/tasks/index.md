# Практичні завдання: Семестр 1 (Незалежний трек)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

Єдина послідовна лінійка практичних завдань 1-го семестру. Усі заготовки та JUnit 5 тести знаходяться в модулі [sem1-exercises](https://github.com/vplanto/java/tree/main/sem1-exercises).

## Порядок виконання

1. Форкніть або склонуйте репозиторій курсу та відкрийте директорію `sem1-exercises` як Maven-проєкт у IntelliJ IDEA.
2. Виконуйте завдання послідовно від `T00` до `T10`.
3. Реалізуйте методи з `TODO` у відповідному пакеті, не змінюючи публічні сигнатури та тести.
4. Переконайтеся, що команда `mvn test -Dtest=<НазваТесту>` проходить без помилок, і здайте посилання на коміт або Pull Request у Google Classroom.

## Реєстр завдань

| # | Завдання | Пов'язана теорія та практикум | Команда перевірки |
| :-: | :--- | :--- | :--- |
| **T00** | [Налаштування середовища, JDK 21, Git та GitHub](t00_setup.md) | [00_intro](../00_intro.md), [p00_git_github](../p00_git_github.md), [n00_java24](../n00_java24.md) | `mvn test -Dtest=EnvironmentSanityTest` |
| **T01** | [Базовий синтаксис, Maven та стиль коду](t01_basics.md) | [01_what_is_java](../01_what_is_java.md), [01a_jvm_lifecycle](../01a_jvm_lifecycle.md), [p01_maven](../p01_maven.md) | `mvn test -Dtest=NumberStatsTest` |
| **T02** | [Класи, інтерфейси, наслідування та контракт `Object`](t02_oop.md) | [01b_jvm_memory_gc](../01b_jvm_memory_gc.md), [p02_class](../p02_class.md) | `mvn test -Dtest=DeviceContractTest` |
| **T03** | [Узагальнення (Generics) та обмеження типів](t03_generics.md) | [p03_generics](../p03_generics.md) | `mvn test -Dtest=GenericsTest` |
| **T04** | [Колекції (`List`, `Set`, `Map`) та однозв'язний список](t04_collections.md) | [02_collections_intro](../02_collections_intro.md), [03_choosing_right_collection](../03_choosing_right_collection.md), [p04_collections](../p04_collections.md) | `mvn test -Dtest=CollectionsTest` |
| **T05** | [Рядки та файловий I/O (`java.nio.file`)](t05_io.md) | [p04_collections](../p04_collections.md) | `mvn test -Dtest=LogAnalyzerTest` |
| **T06** | [Обробка винятків та захист інваріантів](t06_exceptions.md) | [04_java_exceptions](../04_java_exceptions.md) | `mvn test -Dtest=ExceptionsTest` |
| **T07** | [SOLID, DIP та модульний дизайн](t07_solid.md) | [05_solid](../05_solid.md), [p05_solid](../p05_solid.md), [p06_module_design](../p06_module_design.md) | `mvn test -Dtest=SolidDesignTest` |
| **T08** | [Багатопотоковість: синхронізація та `ExecutorService`](t08_concurrency.md) | [06_threads](../06_threads.md), [p07_race_condition](../p07_race_condition.md), [p08_executors](../p08_executors.md) | `mvn test -Dtest=ConcurrencyTest` |
| **T09** | [Командний Git (PR) та JDBC + H2](t09_jdbc.md) | [09_jdbc](../09_jdbc.md), [p09_git](../p09_git.md), [p10_jdbc](../p10_jdbc.md) | `mvn test -Dtest=JdbcRepositoryTest` |
| **T10** | [Патерни проєктування: Factory, Strategy, Decorator](t10_patterns.md) | [10_design_patterns](../10_design_patterns.md), [07_troubleshooting](../07_troubleshooting.md), [08_logging](../08_logging.md) | `mvn test -Dtest=PatternsTest` |
| **FP** | [Фінальний проєкт: Консольна гра (3 етапи)](../n01_final_project.md) | [n01_final_project](../n01_final_project.md), [n05_uml_basics](../n05_uml_basics.md) | власні JUnit-тести проєкту |

[Повернутися до головного меню курсу](../index.md)
