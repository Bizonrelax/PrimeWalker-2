package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс алгоритмов визуализации (порт core.BaseVisualizer, ТЗ: расширяемость
 * через наследование). Подкласс задаёт геометрию трассы в {@link #render(long, int, double)};
 * флаги простоты — детерминированная проверка пробным делением, независимая от кэша prime.*.
 */
public abstract class BaseVisualizer {

    /** Максимальное число шагов по ТЗ (защита от исчерпания ресурсов). */
    public static final int MAX_STEPS = 100_000_000;

    /** Имя алгоритма для выпадающего списка UI и отчётов. */
    public abstract String displayName();

    /**
     * Строит трассу: start — первое число, steps — количество чисел (1..MAX_STEPS),
     * scale — длина шага в единицах сетки (0.1..10.0 по ТЗ).
     */
    public abstract VisualizationResult render(long start, int steps, double scale);

    /** Проверка диапазона steps; общие для всех алгоритмов границы ТЗ. */
    protected static void checkSteps(int steps) {
        if (steps < 1 || steps > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "Шагов должно быть 1.." + MAX_STEPS + ", получено: " + steps);
        }
    }

    /** Проверка диапазона scale (ТЗ: 0.1..10.0). */
    protected static void checkScale(double scale) {
        if (!(scale >= 0.1 && scale <= 10.0)) {
            throw new IllegalArgumentException("Масштаб вне диапазона 0.1..10.0: " + scale);
        }
    }

    /** Детерминированная простота числа (арбитр — ТЗ, поведение не зависит от состояния генератора). */
    protected static boolean isPrimeValue(long v) {
        return PrimeSequences.isPrime(v);
    }

    /** Флаги простоты для значений start..start+steps-1. */
    protected static boolean[] flags(long start, int steps) {
        var f = new boolean[steps];
        for (int i = 0; i < steps; i++) f[i] = isPrimeValue(Math.addExact(start, i));
        return f;
    }

    /** Значения последовательности start..start+steps-1. */
    protected static long[] values(long start, int steps) {
        var v = new long[steps];
        for (int i = 0; i < steps; i++) v[i] = Math.addExact(start, i);
        return v;
    }

    /** Границы массивов координат и сборка результата. */
    protected static VisualizationResult bounds(double[] xs, double[] ys, long[] vals,
                                                boolean[] fl, String algorithm) {
        Bounds b = new Bounds();
        for (int i = 0; i < xs.length; i++) b.extend(xs[i], ys[i]);
        return new VisualizationResult(toPath(xs, ys), fl, vals, b, algorithm);
    }

    /** Список точек из массивов координат. */
    protected static List<Point2D.Double> toPath(double[] xs, double[] ys) {
        var p = new ArrayList<Point2D.Double>(xs.length);
        for (int i = 0; i < xs.length; i++) {
            p.add(new Point2D.Double(xs[i], ys[i]));
        }
        return p;
    }
}
