package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс алгоритмов визуализации (ТЗ: расширяемость через наследование).
 * Шаблонный метод {@link #visualize(long, int)} строит трассу по последовательности
 * чисел start..start+steps-1; подкласс задаёт только геометрию шага
 * {@link #step(long, boolean)} и стартовую точку {@link #startPoint()}.
 * Ограничение ТЗ на число шагов (≤ 100 000 000) проверяется здесь централизованно.
 */
public abstract class BaseVisualizer {

    /** Максимальное число шагов по ТЗ (защита от исчерпания ресурсов). */
    public static final int MAX_STEPS = 100_000_000;

    private final double scale;

    protected BaseVisualizer(double scale) {
        if (!(scale >= 0.1 && scale <= 10.0)) {
            throw new IllegalArgumentException("Масштаб вне диапазона 0.1..10.0: " + scale);
        }
        this.scale = scale;
    }

    public double scale() {
        return scale;
    }

    /** Имя алгоритма для выпадающего списка UI и отчётов. */
    public abstract String name();

    /**
     * Приращение координат на числе value. Флаг isPrime — простое ли value
     * (определяет смену направления/выделение). Значения возвращаются в
     * единицах сетки до применения масштаба.
     */
    protected abstract Point2D.Double step(long value, boolean isPrime);

    /** Стартовая точка трассы в координатах сетки. */
    protected abstract Point2D.Double startPoint();

    /** Простота числа; детерминированная проверка пробным делением. */
    protected boolean isPrime(long value) {
        return PrimeSequences.isPrime(value);
    }

    /**
     * Полный цикл: точки, флаги простоты, значения, границы.
     * Трасса содержит steps+1 точек: точка 0 — стартовая (позиция числа start),
     * точка i+1 — позиция числа start+i после i шагов. Флаги и значения
     * соответствуют позициям чисел: {@code values[i] = start+i},
     * {@code primeFlags[i]} — простота этого числа.
     *
     * @param start начальное число (любое целое по ТЗ)
     * @param steps количество чисел-шагов, 1..MAX_STEPS
     * @return результат, готовый для render.RenderService
     */
    public VisualizationResult visualize(long start, int steps) {
        if (steps < 1 || steps > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "Шагов должно быть 1.." + MAX_STEPS + ", получено: " + steps);
        }
        List<Point2D.Double> path = new ArrayList<>(steps + 1);
        boolean[] flags = new boolean[steps];
        long[] values = new long[steps];
        Bounds bounds = new Bounds();

        Point2D.Double startPt = startPoint();
        double cx = startPt.x, cy = startPt.y;
        // узел 0 — стартовая точка (позиция числа start до первого перехода)
        path.add(new Point2D.Double(cx, cy));
        for (int i = 0; i < steps; i++) {
            long v = start + i;
            if (i > 0) {
                Point2D.Double d = step(v, isPrime(v));
                // масштаб применяется к приращению: длина шага = scale единиц сетки
                cx += d.x * scale;
                cy += d.y * scale;
            }
            path.add(new Point2D.Double(cx, cy));
            bounds.extend(cx, cy);
            flags[i] = isPrime(v);
            values[i] = v;
        }
        return new VisualizationResult(path, flags, values, bounds, name());
    }
}
