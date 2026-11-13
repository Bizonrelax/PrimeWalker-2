---
id: PLAN
file: PLAN.md
etymology: PLAN = план; свод фрагментов ТЗ.
desc: Реестр фрагментов разработки PrimeWalker-2 по ТЗ docs/TZ.md: пакеты, задачи, наследие, критерии готовности, оценка, ci.
name: План фрагментов GVK
F: план
Q: полный
N: [фрагментация, ТЗ]
O: практический
T: активен, v1
---

# План фрагментов PrimeWalker-2

Тип репозитория: программный (ТЗ на программу). Стек: Java 21, Maven, Swing GUI, переносимый EXE через jpackage app-image (см. PROTOCOL.md, Часть 10). Замечание к ТЗ: ТЗ рекомендует Qt/PySide6, протокол владельца задаёт единый стек Java 21 — приоритет у протокола; функциональные требования ТЗ реализуются на Swing/AWT без потери функций (PDF-экспорт — собственный минимальный PDF-writer, печать — javax.print). База данных не вводится: настройки и состояние — обычные файлы (per-user properties + .pwalk JSON), переносимость копированием папки.

Главный класс: `io.github.bizonrelax.primewalker2.Main`. Пакет корня: `io.github.bizonrelax.primewalker2`.

| № | пакеты | задачи ТЗ | наследие | готовность (команда) | оценка ч | TCP-порт | ci |
|---|---|---|---|---|---|---|---|
| 000 | корень проекта (pom.xml, .gitignore, .github/workflows, README-скелет), пакет `io.github.bizonrelax.primewalker2` (Main) | Общие требования: каркас, стек, CI | .gitignore (REFACTOR), README.md (REWRITE), LICENSE (KEEP), docs/TZ.md (KEEP) | `mvn -B -ntp verify` → Failures: 0, Errors: 0 | 3 | — | ci: windows |
| 001 | `...primewalker2.prime` | Генерация простых чисел: интерфейс стратегии PrimeGenerationStrategy, решето Эратосфена, сегментное решето, пробное деление; ограничение шагов ≤ 100 000 000 | нет | `mvn -B -ntp verify` зелёные юнит-тесты PrimeGeneratorTest | 4 | — | |
| 002 | `...primewalker2.core` | Модель визуализаций: BaseVisualizer, UlamSpiralVisualizer, JacobsLadderVisualizer, PrimeWalkVisualizer, цветовая кодировка по модулю N, Bounds-расчёт, модель результата (точки/сегменты) | нет | `mvn -B -ntp verify` зелёные юнит-тесты VisualizerTest | 6 | — | |
| 003 | `...primewalker2.render` | Рендеринг: отрисовка трассы в BufferedImage (QPainter-аналог — Graphics2D), постеризация/тесселяция A4 с полями склейки, монтажные метки (номера листов, Top, registration marks, butt guides), crop marks, расчёт сетки плиток | нет | `mvn -B -ntp verify` зелёные юнит-тесты TilingTest, RenderTest | 8 | — | |
| 004 | `...primewalker2.export` | Экспорт: PNG, JPG, PDF (X-совместимый вывод с bleed 3 мм, crop marks, встраивание шрифтов, векторный вывод), ZIP-архив плиток + assembly_instructions, запись файла с ошибками доступа | нет | `mvn -B -ntp verify` зелёные юнит-тесты ExportTest | 8 | — | |
| 005 | `...primewalker2.persist` | Персистентность: AppSettings (аналог QSettings на java.util.Properties в per-user dir, валидация значений, значения по умолчанию), ProjectManager (.pwalk JSON: версия формата, параметры, цвета, modulo, дата, ссылка на изображение; сохранение/загрузка), история экспортов, глобальная статистика (firstRun, totalRuntimeSec, largest image, totalStepsDrawn, счётчики алгоритмов и форматов) | нет | `mvn -B -ntp verify` зелёные юнит-тесты SettingsTest, ProjectTest, StatsTest | 6 | — | |
| 006 | `...primewalker2.ui` | Интерфейс: главное окно (панель параметров слева: QSpinBox-аналоги JSpinner, QComboBox-аналоги, кнопки цвета, выбор темы light/dark, размер шрифта), фоновый Worker-поток с прогрессом и остановкой, диалог экспорта, пост-отчёт об отрисовке (размер, файл, листы, время, RAM %, шаги; кнопка «Копировать отчёт»), меню Файл/Помощь, восстановление геометрии окна, closeEvent-сохранение | нет | `mvn -B -ntp verify` зелёные (gui-тесты помечены @Tag("gui") и исключены); ручная проверка на Windows | 10 | — | ci: windows |
| 007 | сборка и выпуск | Проверка полного цикла: mvn verify, shade-jar, jpackage app-image на windows-latest, ZIP в Releases по тегу v1.0.0; финальный README по Части 11 | workflow-файл 000 | `mvn -B -ntp verify` + зелёный CI на push тега | 4 | — | ci: windows |

TCP-порты локального запуска не требуются (десктопное приложение без сети). Офлайн-first: никаких сетевых вызовов в рантайме.

Порядок работ: 000 → затем 001–006 параллельно (границы пакетов не пересекаются), 007 после всех.
