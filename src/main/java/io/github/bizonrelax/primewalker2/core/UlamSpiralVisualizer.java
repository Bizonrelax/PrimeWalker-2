package io.github.bizonrelax.primewalker2.core;

/**
 * Спираль Улама (ТЗ, алгоритм 1): числа раскладываются в квадратной спирали против
 * часовой стрелки от центра; ось y — математическая (вверх положительно).
 * Обход: 1=(0,0), 2=(1,0), 3=(1,1), 4=(0,1), 5=(-1,1), 6=(-1,0), 7=(-1,-1), ...
 */
public final class UlamSpiralVisualizer extends BaseVisualizer {

    public static final String NAME = "Ulam Spiral";

    @Override
    public String displayName() { return NAME; }

    @Override
    public VisualizationResult render(long start, int steps, double scale) {
        checkSteps(steps);
        checkScale(scale);
        var xs = new double[steps];
        var ys = new double[steps];
        for (int i = 0; i < steps; i++) {
            long[] p = spiralPos(Math.addExact(start, i));
            xs[i] = p[0] * scale;
            ys[i] = p[1] * scale;
        }
        return bounds(xs, ys, values(start, steps), flags(start, steps), NAME);
    }

    /**
     * Позиция числа n (любое целое: отрицательные отображаются на |n| для
     * непрерывности сетки спирали) в квадратной спирали против часовой, 1=(0,0).
     */
    static long[] spiralPos(long n) {
        long a = Math.abs(n);
        if (a < 1) return new long[]{0, 0};
        if (a == 1) return new long[]{0, 0};
        long ring = (long) Math.floor(Math.sqrt((double) a));
        // кольцо r охватывает числа ((2r-1)^2, (2r+1)^2]; коррекция округления sqrt
        while ((2 * ring + 1) * (2 * ring + 1L) < a) ring++;
        while (ring > 0 && (2 * ring - 1) * (2 * ring - 1L) >= a) ring--;
        long s = 2 * ring;                                  // длина стороны кольца
        long prevMax = (2 * ring - 1) * (2 * ring - 1L);    // последнее число внутреннего кольца
        long idx = a - prevMax;                             // 1..4s — позиция на дуге кольца
        long cx, cy;
        if (idx <= s) {
            cx = ring; cy = -ring + idx;                    // правая колонка вверх
        } else if (idx <= 2 * s) {
            long t = idx - s;
            cx = ring - t; cy = ring;                       // верхняя строка влево
        } else if (idx <= 3 * s) {
            long t = idx - 2 * s;
            cx = -ring; cy = ring - t;                      // левая колонка вниз
        } else {
            long t = idx - 3 * s;
            cx = -ring + t; cy = -ring;                     // нижняя строка вправо
        }
        return new long[]{cx, cy};
    }
}
