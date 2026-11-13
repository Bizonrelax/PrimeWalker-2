---
id: PORTS
file: PORTS.md
etymology: PORTS = публичные сущности.
desc: Реестр публичных типов и методов PrimeWalker-2, на которые ссылаются другие пакеты.
name: Порты GVK
F: реестр
Q: полный
N: [api]
O: служебный
T: активен
---

# Публичные сущности (порт = API-контракт)

| порт | сигнатура | владелец № | зависимые № |
|---|---|---|---|
| prime.PrimeGenerationStrategy | `interface PrimeGenerationStrategy { long[] primesFrom(long start, int count); }` | 001 | 002, 006 |
| prime.PrimeGenerator | `final class PrimeGenerator { PrimeGenerator(PrimeGenerationStrategy s); long[] primesFrom(long start, int steps); }` | 001 | 002, 006 |
| core.VisualizationResult | `record VisualizationResult(double[] xs, double[] ys, boolean[] primeFlags, double minX, double maxX, double minY, double maxY)` | 002 | 003, 004, 006 |
| core.BaseVisualizer | `abstract class BaseVisualizer { abstract VisualizationResult render(long start, int steps, double scale); String displayName(); }` | 002 | 006 |
| core.VisualizerFactory | `static List<BaseVisualizer> all(); static BaseVisualizer byName(String name);` | 002 | 006 |
| core.ColorModuloScheme | `static int colorIndexFor(long n, int modulo)` | 002 | 003, 006 |
| render.RenderService | `BufferedImage renderImage(VisualizationResult r, RenderOptions o)` | 003 | 004, 006 |
| render.TilingPlan | `static TilingPlan forImage(int wPx, int hPx, int dpi, double overlapMm); int sheetsWide(); int sheetsHigh(); int totalSheets();` | 003 | 004, 006 |
| export.ExportService | `ExportOutcome exportPng(...); ExportOutcome exportJpg(...); ExportOutcome exportPdf(...); ExportOutcome exportZipTiles(...)` | 004 | 006 |
| persist.AppSettings | `load(); save(); typed getters/setters по ключам ТЗ (ui/*, window/*, export/history, stats/*, project/*)` | 005 | 006 |
| persist.ProjectManager | `saveProject(Path pwalk, ProjectState s); ProjectState loadProject(Path pwalk)` | 005 | 006 |
| persist.ProjectState | `record ProjectState(int formatVersion, long initialNumber, int stepCount, double scale, String visualizerType, Colors colors, int colorModulo, Instant savedAt, String imagePath)` | 005 | 006 |
| persist.GlobalStats | счётчики firstRun/totalRuntimeSec/largestImage/totalStepsDrawn/per-algorithm/per-format | 005 | 006 |
| ui.MainWindow | конструктор `(AppSettings settings)`; `MainWindow.showAndRun()` вызывается из Main | 006 | 000 |

Формат файла проекта `.pwalk`: JSON UTF-8, поля: `formatVersion` (int, =1), `initial_number`, `step_count`, `scale`, `visualizer_type`, `colors{start,end,path}`, `color_modulo`, `saved_at` (ISO-8601), `image_path`. Владелец — 005.
