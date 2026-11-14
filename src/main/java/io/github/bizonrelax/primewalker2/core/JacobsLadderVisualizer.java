package io.github.bizonrelax.primewalker2.core;

/**
 * Лестница Якова (ТЗ, алгоритм 2): путь идёт вправо на каждом числе; вертикальное
 * направление меняется на противоположное при каждом простом числе. Стартовое
 * направление — вверх (y положительный). Простое меняет направление следующего шага.
 */
public final class JacobsLadderVisualizer extends BaseVisualizer {

    public static final String NAME = "Jacob's Ladder";

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
        int dir = 1; // вверх по сетке (ТЗ, алгоритм 2: направление меняется НА простом числе)
        double y = 0;
        for (int i = 0; i < steps; i++) {
            if (i > 0) {
                if (fl[i]) dir = -dir; // разворот на предшествующем простом, шаг — в новом направлении
                y += dir;              // затем шаг в новом направлении
            }
            xs[i] = i * scale;
            ys[i] = y * scale;
        }
        return bounds(xs, ys, vals, fl, NAME);
    }
}
