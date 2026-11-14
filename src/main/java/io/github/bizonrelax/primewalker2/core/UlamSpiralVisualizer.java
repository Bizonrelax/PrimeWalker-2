package io.github.bizonrelax.primewalker2.core;

/** Спираль Улама: числа против часовой стрелки от центра (ТЗ, алгоритм 1). */
public final class UlamSpiralVisualizer extends BaseVisualizer {

    public static final String NAME = "Ulam Spiral";

    @Override
    public String displayName() { return NAME; }

    @Override
    public VisualizationResult render(long start, int steps, double scale) {
        if (steps <= 0) throw new IllegalArgumentException("steps > 0");
        var xs = new double[steps];
        var ys = new double[steps];
        var vals = new long[steps];
        var fl = new boolean[steps];
        int x = 0, y = 0, dx = 0, dy = -1;
        for (int i = 0; i < steps; i++) {
            if (i > 0) {
                // поворот против часовой на углах слоёв спирали
                if (x == y || (x == -y && x > 0) || (x == -y + 1 && x <= 0)) {
                    int t = dx; dx = -dy; dy = t;
                }
                x += dx; y += dy;
            }
            xs[i] = x * scale; ys[i] = y * scale;
            vals[i] = Math.addExact(start, i);
            fl[i] = Primes.isPrime(vals[i]);
        }
        return bounds(xs, ys, vals, fl);
    }
}
