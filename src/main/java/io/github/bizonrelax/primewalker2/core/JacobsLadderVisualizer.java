package io.github.bizonrelax.primewalker2.core;

/** Лестница Якова: направление вверх/вниз меняется на каждом простом числе (ТЗ, алгоритм 2). */
public final class JacobsLadderVisualizer extends BaseVisualizer {

    public static final String NAME = "Jacob's Ladder";

    @Override
    public String displayName() { return NAME; }

    @Override
    public VisualizationResult render(long start, int steps, double scale) {
        if (steps <= 0) throw new IllegalArgumentException("steps > 0");
        var xs = new double[steps];
        var ys = new double[steps];
        var vals = new long[steps];
        var fl = new boolean[steps];
        int dir = -1; // стартовое направление — вниз (экранная ось Y растёт вниз, как в UlamSpiralVisualizer)
        double lvl = 0;
        for (int i = 0; i < steps; i++) {
            vals[i] = Math.addExact(start, i);
            fl[i] = Primes.isPrime(vals[i]);
            xs[i] = i * scale;
            ys[i] = lvl * scale; // шаг лестницы равен масштабу ТЗ, как в других визуализациях
            if (fl[i]) { dir = -dir; }
            lvl += dir;
        }
        return bounds(xs, ys, vals, fl);
    }
}
