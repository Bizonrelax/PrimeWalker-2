package io.github.bizonrelax.primewalker2.core;

import java.awt.geom.Point2D;

/**
 * Спираль Улама (ТЗ, алгоритм 1): числа раскладываются в квадратной спирали против
 * часовой стрелки, начиная от центра; простые выделяются. Точки трассы — узлы сетки,
 * соответствующие числам start..start+steps-1.
 */
public final class UlamSpiralVisualizer extends BaseVisualizer {

    public UlamSpiralVisualizer() {
        this(1.0);
    }

    public UlamSpiralVisualizer(double scale) {
        super(scale);
    }

    @Override
    public String name() {
        return "Ulam Spiral";
    }

    @Override
    protected Point2D.Double startPoint() {
        return new Point2D.Double(0, 0);
    }

    /**
     * Приращение от позиции числа value-1 к позиции value в спирали.
     * Ось y экранных координат (вниз положительна): вверх по экрану — отрицательный dy.
     */
    @Override
    protected Point2D.Double step(long value, boolean isPrime) {
        if (value <= 1) {
            // первое число уже в стартовой точке — смещения нет
            return new Point2D.Double(0, 0);
        }
        long[] cur = spiralPos(value);
        long[] prev = spiralPos(value - 1);
        return new Point2D.Double(cur[0] - prev[0], -(cur[1] - prev[1]));
    }

    /**
     * Позиция числа n>=1 в квадратной спирали против часовой, центр 1=(0,0), ось y вверх.
     * Обход: 1=(0,0), 2=(1,0) право, 3=(1,1) вверх, 4=(0,1), 5=(-1,1), 6=(-1,0),
     * 7=(-1,-1), 8=(0,-1), 9=(1,-1), ...
     */
    static long[] spiralPos(long n) {
        if (n < 1) throw new IllegalArgumentException("n >= 1, получено " + n);
        if (n == 1) return new long[]{0, 0};
        long ring = (long) Math.floor(Math.sqrt((double) n));
        // кольцо r охватывает числа ((2r-1)^2, (2r+1)^2]; коррекция округления sqrt
        while ((2 * ring + 1) * (2 * ring + 1L) < n) ring++;
        while (ring > 0 && (2 * ring - 1) * (2 * ring - 1L) >= n) ring--;
        long s = 2 * ring;                                  // длина стороны кольца
        long prevMax = (2 * ring - 1) * (2 * ring - 1L);    // последнее число внутреннего кольца
        long idx = n - prevMax;                             // 1..4s — позиция на дуге кольца
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
