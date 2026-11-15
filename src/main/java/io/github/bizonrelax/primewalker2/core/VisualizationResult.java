package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Модель результата визуализации (ТЗ: точки/сегменты): путь в координатах сетки,
 * флаги простоты каждой точки, сами числа и границы трассы.
 * Порт core.VisualizationResult — потребители: render.RenderService, export, ui.
 *
 * <p>Соглашение об индексах: {@code path.get(i)}, {@code primeFlags[i]} и
 * {@code values[i]} соответствуют одному и тому же числу {@code values[i]}
 * (конвенция выбрана порт-совместимой с веткой gvk/006, где служебных точек нет).
 *
 * @param path      точки трассы (не пустой)
 * @param primeFlags простота числа в соответствующей точке; длина = path.size()
 * @param values    числовые значения последовательности в точках; та же длина
 * @param bounds    ограничивающий прямоугольник трассы
 * @param algorithm имя алгоритма визуализации (для отчётов и статистики)
 */
public record VisualizationResult(List<Point2D.Double> path, boolean[] primeFlags,
                                  long[] values, Bounds bounds, String algorithm) {

    public VisualizationResult {
        if (path == null || primeFlags == null || values == null || bounds == null) {
            throw new NullPointerException("Компоненты результата не могут быть null");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Путь не может быть пустым");
        }
        if (primeFlags.length != values.length) {
            throw new IllegalArgumentException("Длины primeFlags/values должны совпадать");
        }
        if (path.size() != primeFlags.length) {
            throw new IllegalArgumentException(
                    "Длина path должна совпадать с длиной primeFlags/values");
        }
        algorithm = algorithm == null ? "" : algorithm;
    }

    /** Число точек трассы. */
    public int size() {
        return path.size();
    }

    /** Сколько чисел последовательности простые. */
    public int primeCount() {
        int c = 0;
        for (boolean b : primeFlags) {
            if (b) c++;
        }
        return c;
    }

    // convenience-аксессоры границ (потребители ui/render читают их напрямую)

    public double minX() { return bounds.minX(); }

    public double maxX() { return bounds.maxX(); }

    public double minY() { return bounds.minY(); }

    public double maxY() { return bounds.maxY(); }
}
