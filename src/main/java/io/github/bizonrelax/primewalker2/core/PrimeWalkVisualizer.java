package io.github.bizonrelax.primewalker2.core;

/**
 * Prime Walk: направление шага задаётся последней цифрой простого числа
 * (ТЗ, алгоритм 3): 1 и 7 — влево, 3 и 9 — вправо, 5 — вверх, остальное — вниз.
 */
public final class PrimeWalkVisualizer extends BaseVisualizer {

    public static final String NAME = "Prime Walk";

    @Override
    public String displayName() { return NAME; }

    static int[] stepFor(long n) {
        if (!Primes.isPrime(n) || n == 2 || n == 5) return new int[]{0, 0};
        return switch ((int) (n % 10)) {
            case 1, 7 -> new int[]{-1, 0};
            case 3, 9 -> new int[]{1, 0};
            case 5 -> new int[]{0, 1};
            default -> new int[]{0, -1}; // цифра 2 или 4,6,8 у простых не встречается, кроме 2
        };
    }

    @Override
    public VisualizationResult render(long start, int steps, double scale) {
        if (steps <= 0) throw new IllegalArgumentException("steps > 0");
        var xs = new double[steps];
        var ys = new double[steps];
        var vals = new long[steps];
        var fl = new boolean[steps];
        double x = 0, y = 0;
        for (int i = 0; i < steps; i++) {
            vals[i] = Math.addExact(start, i);
            fl[i] = Primes.isPrime(vals[i]);
            xs[i] = x * scale;
            ys[i] = y * scale;
            int[] d = stepFor(vals[i]);
            x += d[0];
            y += d[1];
        }
        return bounds(xs, ys, vals, fl);
    }
}
