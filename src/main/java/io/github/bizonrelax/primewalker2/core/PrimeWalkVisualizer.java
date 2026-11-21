package io.github.bizonrelax.primewalker2.core;

/**
 * Ход простого числа (ТЗ, алгоритм 3, ревизия 2 / CHANGE-001): двумерный блуждающий
 * ход — направление шага задаётся последней цифрой простого числа (кроме 2 и 5,
 * которые не дают смещения): 1 — влево (-1;0), 3 — вверх (0;+1), 7 — вправо (+1;0),
 * 9 — вниз (0;-1); составные числа шаг не двигают. Ось y — математическая.
 * Две ортогональные оси смещения исключают вырождение трассы в узкую полосу (ТЗ: дефект).
 */
public final class PrimeWalkVisualizer extends BaseVisualizer {

    public static final String NAME = "Prime Walk";

    /** Смещение (dx, dy) после числа n: ноль для составных, простых 2 и 5 и цифры 5. */
    static int[] stepFor(long n) {
        if (!PrimeSequences.isPrime(n) || n == 2 || n == 5) return new int[]{0, 0};
        return switch ((int) Math.floorMod(n, 10)) {
            case 1 -> new int[]{-1, 0};   // влево
            case 3 -> new int[]{0, 1};    // вверх (ось y математическая)
            case 7 -> new int[]{1, 0};    // вправо
            case 9 -> new int[]{0, -1};   // вниз
            default -> new int[]{0, 0};   // 5 у простых — игнор (ТЗ: кроме 2 и 5)
        };
    }

    @Override
    public String displayName() { return NAME; }

    @Override
    public VisualizationResult render(long start, int steps, double scale) {
        checkSteps(steps);
        checkScale(scale);
        var xs = new double[steps];
        var ys = new double[steps];
        var vals = values(start, steps);
        var fl = flags(start, steps);
        double x = 0, y = 0;
        for (int i = 0; i < steps; i++) {
            xs[i] = x * scale;
            ys[i] = y * scale;
            int[] d = stepFor(vals[i]);
            x += d[0];
            y += d[1];
        }
        return bounds(xs, ys, vals, fl, NAME);
    }
}
