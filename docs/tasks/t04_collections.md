# Завдання T04: Колекції (`List`, `Set`, `Map`) та однозв'язний список

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 2](../02_collections_intro.md), [Лекція 3](../03_choosing_right_collection.md), [Практикум 4](../p04_collections.md)
- **Код і тести:** [t04_collections](https://github.com/vplanto/java/tree/main/sem1-exercises/src/main/java/ua/edu/onu/java/sem1/t04_collections)
- **Команда перевірки:** `mvn test -Dtest=CollectionsTest`

## Мета

Зрозуміти внутрішню будову зв'язного списку (посилання `head`/`tail`, складність `O(1)` проти `O(n)`) та навчитися комбінувати `List`, `Set` і `Map` для агрегації даних.

## Що потрібно реалізувати

1. У класі `SimpleLinkedList<T>`:
   - `addLast(T element)` - вставка в кінець за `O(1)` з оновленням `head`, `tail`, `size`;
   - `get(int index)` - доступ за індексом із перевіркою меж (`IndexOutOfBoundsException`);
   - `removeFirst()` - видалення першого елемента за `O(1)` (`NoSuchElementException` для порожнього списку, обнулення `tail` при спорожненні).
2. У класі `GradeBook`:
   - `addGrade(String student, int score)` - валідація `score` в `[0, 100]` і непорожнього імені;
   - `getHistory()` - незмінна копія списку всіх записів;
   - `getUniqueStudents()` - множина унікальних студентів (`Set<String>`);
   - `getAverageByStudent()` - середній бал кожного студента (`Map<String, Double>`).

## Критерії приймання

- Усі тести в `CollectionsTest` зелені.
- `getHistory()` захищений від зовнішньої модифікації (`List.copyOf`).

[До реєстру завдань](index.md)
