# PrimeWalker-2

[English](#english) | [Русский](#русский)

## English

### What it is

PrimeWalker-2 is an offline desktop application that turns sequences of prime numbers into large-format visual artworks (Ulam spiral, Jacob's ladder, prime walk) and exports them as PNG/JPG/PDF/ZIP ready for poster printing.

> **Status: release candidate v1.0.0 (fragments 000–006 integrated on `gvk/007`).** All core functionality from the project brief (`docs/TZ.md`) is implemented and covered by tests (64/64 green). Closing merge into `main` is performed per GVK protocol Part 7.

### Who it is for

Print-shop operators preparing prime-number posters; mathematicians and enthusiasts who want a printable visualization of prime distributions.

### Why

Manual plotting and pre-press preparation of multi-sheet posters is error-prone; PrimeWalker-2 automates generation, tiling, bleed/crop marks and packaging in one offline tool.

### Features

Planned (per `docs/TZ.md`): prime generation strategies (sieve of Eratosthenes, segmented sieve, trial division), Ulam spiral / Jacob's ladder / prime walk visualizations, color coding by modulo N, A4 poster tiling with overlap and assembly marks, export to PNG/JPG/print-ready PDF/ZIP, project save/load (.pwalk), settings persistence, global statistics, light/dark themes.

### Requirements

Windows 11 x64. Java is **not** required — the runtime is bundled inside the portable folder.

### Download

Releases page → `PrimeWalker-2-windows-portable.zip`.

### Installation

Unzip the archive into any folder. No installation; portable by copying the folder.

### Quick start

1. Unzip the portable folder and run `PrimeWalker-2\bin\PrimeWalker-2.exe`.
2. Pick a visualization (Ulam spiral / Jacob's ladder / prime walk), a prime generation strategy and a color-modulo scheme in the toolbar.
3. Press Render; pan/zoom the canvas to inspect the pattern.
4. Press Export, choose PNG/JPG/PDF or multi-sheet ZIP (A4 tiling with bleed and crop marks plus assembly instructions) and save.

### User manual

The main window has three areas: settings panel (left), canvas (center), status bar (bottom). Project save/load (`.pwalk` JSON) is under File menu; settings and global statistics persist automatically between sessions; light/dark theme is switchable in View menu.

### Examples

Try `spiral, modulo 6` starting at 2 with ~10,000 steps — the classic Ulam diagonal clustering appears; export as PDF A4 tiles for poster printing.

### Data storage

Settings live in a per-user file; projects are `.pwalk` JSON files chosen by the user. Details in the user manual.

### Troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| App does not start | Missing Visual C++ runtime or corrupted unzip | Re-download the ZIP and extract again |

### FAQ

To be completed at closing.

### Limitations

Current build is the full v1.0.0 candidate (GUI included). Maximum steps per render: 100,000,000 (per TZ). PDF/X-4 certification is unavailable on this stack; export produces a print-ready PDF with bleed and crop marks (see AUDIT.md decision 2). Portable image must be built on Windows for the final `.exe` ZIP artifact.

### Uninstall

Delete the folder.

### License

Public Domain (see `LICENSE`).

---

## Русский

### Что это

PrimeWalker-2 — офлайн-настольное приложение, превращающее последовательности простых чисел в крупноформатные визуальные работы (спираль Улама, лестница Иакова, prime walk) и экспортирующее их в PNG/JPG/PDF/ZIP, готовых к постерной печати.

> **Статус: кандидат релиза v1.0.0 (фрагменты 000–006 интегрированы в `gvk/007`).** Вся функциональность по техническому заданию (`docs/TZ.md`) реализована и покрыта тестами (64/64 зелёные). Закрывающее слияние в `main` выполняется по Части 7 протокола GVK.

### Для кого

Операторы печатных салонов, готовящие многосоставные постеры с простыми числами; математики и энтузиасты, которым нужна печатная визуализация распределения простых чисел.

### Зачем

Ручная постройка и допечатная подготовка многосоставного постера ошибкоёмки; PrimeWalker-2 автоматизирует генерацию, нарезку на листы, поля склейки и упаковку в одном офлайн-инструменте.

### Возможности

По плану (`docs/TZ.md`): стратегии генерации простых чисел (решето Эратосфена, сегментное решето, пробное деление), визуализации «спираль Улама», «лестница Иакова», «prime walk», цветовая кодировка по модулю N, нарезка на листы A4 с полями склейки и монтажными метками, экспорт в PNG/JPG/PDF для печати/ZIP, сохранение и загрузка проектов (.pwalk), хранение настроек, глобальная статистика, светлая/тёмная темы.

### Требования

Windows 11 x64. Java **не нужна** — рантайм встроен в переносимую папку.

### Скачать

Страница Releases → `PrimeWalker-2-windows-portable.zip`.

### Установка

Распакуйте архив в любую папку. Установка не требуется; перенос копированием папки.

### Быстрый старт

1. Распакуйте переносимую папку и запустите `PrimeWalker-2\bin\PrimeWalker-2.exe`.
2. Выберите визуализацию (спираль Улама / лестница Иакова / prime walk), стратегию генерации простых чисел и цветовую схему по модулю на панели настроек.
3. Нажмите «Отрисовать»; перемещайте и масштабируйте холст для изучения узора.
4. Нажмите «Экспорт» и сохраните PNG/JPG/PDF или многосоставной ZIP (нарезка A4 с полями склейки, монтажными метками и инструкцией по сборке).

### Руководство пользователя

Главное окно состоит из трёх областей: панель настроек (слева), холст (центр), строка состояния (снизу). Сохранение/загрузка проектов (JSON `.pwalk`) — в меню «Файл»; настройки и глобальная статистика сохраняются автоматически между сессиями; светлая/тёмная тема переключается в меню «Вид».

### Примеры

Попробуйте «спираль, модуль 6», старт от 2, ~10 000 шагов — классическое диагональное скопление Улама; экспортируйте в PDF-плитки A4 для печати постера.

### Хранение данных

Настройки хранятся в файле пользователя; проекты — в выбираемых пользователем JSON-файлах `.pwalk`. Подробности — в руководстве пользователя.

### Проблемы

| Симптом | Причина | Решение |
|---|---|---|
| Приложение не запускается | Повреждённая распаковка | Скачайте ZIP заново и распакуйте повторно |

### Вопросы

Будут дополнены при закрытии ТЗ.

### Ограничения

Текущая сборка — полный кандидат v1.0.0 (с GUI). Максимум шагов на отрисовку: 100 000 000 (по ТЗ). Сертификация PDF/X-4 недоступна на данном стеке; экспорт формирует печатно-готовый PDF с полями склейки и монтажными метками (решение 2 в AUDIT.md). Финальный переносимый образ (.exe) собирается на Windows.

### Удаление

Удалите папку.

### Лицензия

Public Domain (см. `LICENSE`).
