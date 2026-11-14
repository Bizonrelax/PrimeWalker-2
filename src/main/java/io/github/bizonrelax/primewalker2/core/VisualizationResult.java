package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Модель результата визуализации (ТЗ: точки/сегменты): путь в координатах сетки,
 * флаги простоты каждой точки, сами числа и границы трассы.
 * Порт core.VisualizationResult — потребители: render.RenderService, export, ui.
 *
 * <p>Соглашение об индексах: {@code path} содержит на одну точку больше, чем
 * {@code primeFlags} и {@code values}: нулевая точка — служебная стартовая
 * (позиция числа {@code start} до первого перехода), точка {@code i+1}
 * соответствует числу {@code values[i]} с флагом {@code primeFlags[i]}.
 *
 * @param path       точки трассы (не пустой, первая — стартовая)
 * @param primeFlags простота числа в соответствующей точке; длина = path.size()-1
 * @param values     числовые значения последовательности в точках; та же длина
 * @param bounds     ограничивающий прямоугольник трассы
 * @param algorithm  имя алгоритма визуализации (для отчётов и статистики)
 */
public record VisualizationResult(List<Point2D.Double> path, boolean[] primeFlags,
                                  long[] values, Bounds bounds, String algorithm) {

    public VisualizationResult {
        if (path == null || primeFlags == null || values == null || bounds == null) {
            throw new NullPointerException("Компоненты результата не могут быть null");
        }
        if (primeFlags.length != values.length) {
            throw new IllegalArgumentException(
                    "Длины primeFlags/values должны совпадать");
        }
        if (path.size() != primeFlags.length + 1) {
            throw new IllegalArgumentException(
                    "Длина path должна быть на 1 больше длины primeFlags/values");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Путь не может быть пустым");
        }
        algorithm = algorithm == null ? "" : algorithm;
    }

    /** Число точек трассы (включая стартовую). */
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
}
