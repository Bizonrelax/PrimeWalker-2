package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;

/**
 * Лестница Якова (ТЗ, алгоритм 2): путь движется вправо на каждый шаг; по вертикали
 * направление меняется на противоположное при каждом простом числе.
 * Состояние направления — поле экземпляра: visualize() создаёт новый трек,
 * повторные вызовы не наследуют направление предыдущего прогона.
 */
public final class JacobsLadderVisualizer extends BaseVisualizer {

    /** +1 вверх по экрану (y уменьшается), -1 вниз. */
    private int direction = -1;

    public JacobsLadderVisualizer() {
        this(1.0);
    }

    public JacobsLadderVisualizer(double scale) {
        super(scale);
    }

    @Override
    public String name() {
        return "Jacob's Ladder";
    }

    @Override
    protected Point2D.Double startPoint() {
        return new Point2D.Double(0, 0);
    }

    @Override
    public VisualizationResult visualize(long start, int steps) {
        direction = -1; // сброс состояния перед прогоном
        return super.visualize(start, steps);
    }

    @Override
    protected Point2D.Double step(long value, boolean isPrime) {
        if (isPrime) {
            direction = -direction;
        }
        // dx=1 вправо; dy экранный: direction=+1 (вверх по сетке) => y уменьшается
        return new Point2D.Double(1, -direction);
    }
}
