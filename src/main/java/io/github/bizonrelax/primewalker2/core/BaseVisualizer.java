package io.github.bizonrelax.primewalker2.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс визуализации (порт core.BaseVisualizer, фрагмент 002):
 * преобразует последовательность чисел в трассу точек на сетке.
 */
public abstract class BaseVisualizer {

    /** Результат: координаты сетки, значения, флаги простоты, ограничивающий прямоугольник. */
    public record VisualizationResult(double[] xs, double[] ys, long[] values, boolean[] primeFlags,
                                      double minX, double maxX, double minY, double maxY) {

        public int size() { return values.length; }

        /** Точки трассы в виде List<Point2D.Double> — вход для render.RenderService. */
        public List<java.awt.geom.Point2D.Double> path() {
            var p = new ArrayList<java.awt.geom.Point2D.Double>(xs.length);
            for (int i = 0; i < xs.length; i++) p.add(new java.awt.geom.Point2D.Double(xs[i], ys[i]));
            return p;
        }
    }

    /** Имя для выпадающего списка UI (совпадает с ключами настроек ТЗ). */
    public abstract String displayName();

    /** Строит трассу: start — первое число, steps — количество чисел, scale — шаг между клетками. */
    public abstract VisualizationResult render(long start, int steps, double scale);

    /** Массив флагов простоты для чисел трассы. */
    protected static boolean[] flags(long[] nums, PrimeCheck check) {
        var f = new boolean[nums.length];
        for (int i = 0; i < nums.length; i++) f[i] = check.isPrime(nums[i]);
        return f;
    }

    /** Абстракция проверки простоты: реализация делегирует пакету prime (порт владельца 001). */
    public interface PrimeCheck { boolean isPrime(long n); }

    protected static VisualizationResult bounds(double[] xs, double[] ys, long[] values, boolean[] flags) {
        double mnx = Double.MAX_VALUE, mxx = -Double.MAX_VALUE, mny = Double.MAX_VALUE, mxy = -Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            mnx = Math.min(mnx, xs[i]); mxx = Math.max(mxx, xs[i]);
            mny = Math.min(mny, ys[i]); mxy = Math.max(mxy, ys[i]);
        }
        if (xs.length == 0) { mnx = mxx = mny = mxy = 0; }
        return new VisualizationResult(xs, ys, values, flags, mnx, mxx, mny, mxy);
    }
}
