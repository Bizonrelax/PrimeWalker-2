package io.github.bizonrelax.primewalker2.render;

import java.awt.Color;

/**
 * Параметры рендеринга трассы (ТЗ: цвета точек/трассы, модуль N, масштаб DPI).
 *
 * @param widthPx        ширина выходного изображения в пикселях (&gt;0)
 * @param heightPx       высота выходного изображения в пикселях (&gt;0)
 * @param dpi            разрешение, пикселей на дюйм (&gt;0), по умолчанию 300
 * @param background     цвет фона
 * @param primeColor     цвет простых точек
 * @param endColor       цвет конечной точки
 * @param pathColor      цвет линии трассы
 * @param modulo         цветовая кодировка по модулю N; 0 — выключена (1..8 по ТЗ)
 * @param pointSizePx    диаметр точки простой, пикселей (&gt;0)
 * @param drawPath       рисовать ли соединяющую линию трассы
 */
public record RenderOptions(int widthPx, int heightPx, int dpi, Color background,
                            Color primeColor, Color endColor, Color pathColor,
                            int modulo, int pointSizePx, boolean drawPath) {

    public RenderOptions {
        if (widthPx <= 0 || heightPx <= 0) {
            throw new IllegalArgumentException("Размеры изображения должны быть положительными");
        }
        if (dpi <= 0) {
            throw new IllegalArgumentException("DPI должен быть положительным");
        }
        if (modulo < 0 || modulo > 16) {
            throw new IllegalArgumentException("Modulo вне диапазона 0..16");
        }
        if (pointSizePx <= 0) {
            throw new IllegalArgumentException("Размер точки должен быть положительным");
        }
        if (background == null || primeColor == null || endColor == null || pathColor == null) {
            throw new NullPointerException("Цвета не могут быть null");
        }
    }

    /** Конструктор по умолчанию для заданного размера холста: белый фон, красный/синий/серый. */
    public static RenderOptions defaults(int widthPx, int heightPx) {
        return new RenderOptions(widthPx, heightPx, 300, Color.WHITE,
                Color.RED, Color.BLUE, Color.GRAY, 0, 2, true);
    }
}
