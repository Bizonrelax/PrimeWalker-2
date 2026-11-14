package io.github.bizonrelax.primewalker2.core;

/**
 * Ограничивающий прямоугольник трассы в координатах сетки (ТЗ: Bounds-расчёт).
 * Инкапсулирует накопление min/max по мере построения пути.
 */
public final class Bounds {

    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private int count;

    /** Расширить границы точкой (x, y). */
    public void extend(double x, double y) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        count++;
    }

    public double minX() { return count == 0 ? 0 : minX; }

    public double minY() { return count == 0 ? 0 : minY; }

    public double maxX() { return count == 0 ? 0 : maxX; }

    public double maxY() { return count == 0 ? 0 : maxY; }

    /** Ширина области (единиц сетки); 0 для пустой трассы. */
    public double width() { return count == 0 ? 0 : maxX - minX; }

    /** Высота области (единиц сетки); 0 для пустой трассы. */
    public double height() { return count == 0 ? 0 : maxY - minY; }

    /** Количество накопленных точек. */
    public int pointCount() { return count; }

    /** Пустая ли область. */
    public boolean isEmpty() { return count == 0; }
}
