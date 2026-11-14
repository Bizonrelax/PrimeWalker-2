package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;

/**
 * Ход простого числа (ТЗ, алгоритм 3): направление шага задаётся последней цифрой
 * простого числа; составные — шаг вправо (нейтральный). Правило ТЗ:
 * цифра 1 или 7 — влево, 3 или 9 — вправо, остальные простые цифры (2,5) — вверх.
 * Композиция направлений даёт запутанные траектории без накопления состояния.
 */
public final class PrimeWalkVisualizer extends BaseVisualizer {

    public PrimeWalkVisualizer() {
        this(1.0);
    }

    public PrimeWalkVisualizer(double scale) {
        super(scale);
    }

    @Override
    public String name() {
        return "Prime Walk";
    }

    @Override
    protected Point2D.Double startPoint() {
        return new Point2D.Double(0, 0);
    }

    @Override
    protected Point2D.Double step(long value, boolean isPrime) {
        if (!isPrime) {
            return new Point2D.Double(1, 0); // составное: нейтральный шаг вправо
        }
        long lastDigit = Math.floorMod(value, 10);
        return switch ((int) lastDigit) {
            case 1, 7 -> new Point2D.Double(-1, 0);          // влево
            case 3, 9 -> new Point2D.Double(1, 0);           // вправо
            default   -> new Point2D.Double(0, -1);          // 2,5: вверх (экранный dy)
        };
    }
}
