package io.github.bizonrelax.primewalker2.render;

/**
 * Геометрическая поза трассы: координаты в единицах сетки визуализатора
 * (без привязки к пикселям). Аналог QTransform-композита: масштаб + сдвиг.
 *
 * @param scaleX умножение x-координаты сетки в пиксели
 * @param scaleY умножение y-координаты сетки в пиксели
 * @param shiftX сдвиг по x, пикселей
 * @param shiftY сдвиг по y, пикселей
 */
public record Pose(double scaleX, double scaleY, double shiftX, double shiftY) {

    public Pose {
        if (scaleX <= 0 || scaleY <= 0) {
            throw new IllegalArgumentException("Масштаб должен быть положительным");
        }
    }

    /** Пиксельная x-координата для координаты сетки gx. */
    public double px(double gx) {
        return gx * scaleX + shiftX;
    }

    /** Пиксельная y-координата для координаты сетки gy. */
    public double py(double gy) {
        return gy * scaleY + shiftY;
    }

    /** Сдвиг этой позы на (dx, dy) пикселей (используется плитками для вырезания области). */
    public Pose translated(double dx, double dy) {
        return new Pose(scaleX, scaleY, shiftX + dx, shiftY + dy);
    }
}
