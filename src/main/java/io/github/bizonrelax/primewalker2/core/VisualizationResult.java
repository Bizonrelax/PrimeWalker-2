package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Модель результата визуализации (ТЗ: точки/сегменты): путь в координатах сетки,
 * флаги простоты каждой точки, сами числа и границы трассы.
 * Порт core.VisualizationResult — потребители: render.RenderService, export, ui.
 *
 * @param path       точки трассы (не пустой, первая — стартовая)
 * @param primeFlags простота числа в соответствующей точке; те же индексы, что и path
 * @param values     числовые значения последовательности в точках; те же индексы
 * @param bounds     ограничивающий прямоугольник трассы
 * @param algorithm  имя алгоритма визуализации (для отчётов и статистики)
 */
public record VisualizationResult(List<Point2D.Double> path, boolean[] primeFlags,
                                  long[] values, Bounds bounds, String algorithm) {

    public VisualizationResult {
        if (path == null || primeFlags == null || values == null || bounds == null) {
            throw new NullPointerException("Компоненты результата не могут быть null");
        }
        if (primeFlags.length != path.size() || values.length != path.size()) {
            throw new IllegalArgumentException(
                    "Длины path/primeFlags/values должны совпадать");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Путь не может быть пустым");
        }
        algorithm = algorithm == null ? "" : algorithm;
    }

    /** Число точек трассы. */
    public int size() {
        return path.size();
    }

    /** Сколько точек помечены простыми. */
    public int primeCount() {
        int c = 0;
        for (boolean b : primeFlags) {
            if (b) c++;
        }
        return c;
    }
}
