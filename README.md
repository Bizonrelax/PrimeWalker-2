# PrimeWalker-2

[English](#english) | [Русский](#русский)

## English

### What it is

PrimeWalker-2 is an offline desktop application that turns sequences of prime numbers into large-format visual artworks (Ulam spiral, Jacob's ladder, prime walk) and exports them as PNG/JPG/PDF/ZIP ready for poster printing.

> **Status: framework skeleton (fragment 000).** The features below are specified by the project brief (`docs/TZ.md`) and are being implemented fragment by fragment. This section will be completed as functionality lands.

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

To be documented when the UI (fragment 006) is merged.

### User manual

To be documented when the UI (fragment 006) is merged.

### Examples

To be documented when rendering/export (fragments 003–004) are merged.

### Data storage

Settings live in a per-user file; projects are `.pwalk` JSON files chosen by the user. Details in the user manual.

### Troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| App does not start | Missing Visual C++ runtime or corrupted unzip | Re-download the ZIP and extract again |

### FAQ

To be completed at closing.

### Limitations

Current build is a skeleton: no GUI yet. Maximum steps per render: 100,000,000 (per TZ).

### Uninstall

Delete the folder.

### License

Public Domain (see `LICENSE`).

---

## Русский

### Что это

PrimeWalker-2 — офлайн-настольное приложение, превращающее последовательности простых чисел в крупноформатные визуальные работы (спираль Улама, лестница Иакова, prime walk) и экспортирующее их в PNG/JPG/PDF/ZIP, готовых к постерной печати.

> **Статус: каркас (фрагмент 000).** Функциональность ниже задана техническим заданием (`docs/TZ.md`) и реализуется по фрагментам; раздел будет дополнен по мере готовности.

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

Будет описан после слияния интерфейса (фрагмент 006).

### Руководство пользователя

Будет описано после слияния интерфейса (фрагмент 006).

### Примеры

Будут добавлены после слияния рендеринга и экспорта (фрагменты 003–004).

### Хранение данных

Настройки хранятся в файле пользователя; проекты — в выбираемых пользователем JSON-файлах `.pwalk`. Подробности — в руководстве пользователя.

### Проблемы

| Симптом | Причина | Решение |
|---|---|---|
| Приложение не запускается | Повреждённая распаковка | Скачайте ZIP заново и распакуйте повторно |

### Вопросы

Будут дополнены при закрытии ТЗ.

### Ограничения

Текущая сборка — каркас: графического интерфейса ещё нет. Максимум шагов на отрисовку: 100 000 000 (по ТЗ).

### Удаление

Удалите папку.

### Лицензия

Public Domain (см. `LICENSE`).
