# Завдання T00: Налаштування середовища, JDK 21, Git та GitHub

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів - у [DISCLAIMER.md](../DISCLAIMER.md).

- **Пов'язані матеріали:** [Лекція 0](../00_intro.md), [Практикум 0](../p00_git_github.md), [Застереження щодо Java LTS](../n00_java24.md)
- **Стартовий модуль:** [sem1-exercises](https://github.com/vplanto/java/tree/main/sem1-exercises)
- **Команда перевірки:** `mvn test -Dtest=EnvironmentSanityTest`

## Мета

Підготувати локальне робоче середовище (JDK 21 LTS, Maven, Git, IntelliJ IDEA) та налаштувати власний GitHub-репозиторій для здачі всіх завдань семестру.

## Що потрібно зробити

1. Встановіть JDK 21 LTS, Apache Maven 3.8+ та Git.
2. Створіть власний приватний або публічний репозиторій на GitHub на основі директорії `sem1-exercises`.
3. Відкрийте `sem1-exercises` у IntelliJ IDEA та запустіть перевірку середовища:
   ```bash
   mvn test -Dtest=EnvironmentSanityTest
   ```
4. Додайте у корінь свого репозиторію файл `STUDENT.md` із ПІБ, групою та виводом команд `java -version` і `mvn -version`.
5. Зафіксуйте зміни комітом `feat(t00): setup environment` та відправте у віддалений репозиторій (`git push`).

## Критерії приймання

- Тест `EnvironmentSanityTest` проходить успішно на JDK 21+.
- У репозиторії є коректний `.gitignore` (директорії `target/` та `.idea/` не потрапляють у Git).
- У Classroom прикріплено посилання на репозиторій та коміт `t00`.

[До реєстру завдань](index.md)
