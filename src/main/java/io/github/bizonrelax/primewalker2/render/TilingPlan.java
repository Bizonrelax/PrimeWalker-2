package io.github.bizonrelax.primewalker2.render;

/**
 * План тесселяции постера на листы A4 (ТЗ: 210x297 мм, поле склейки 10 мм,
 * неизвлекаемые поля принтера по 5 мм со всех сторон). Порт render.TilingPlan.
 *
 * Модель координат: "постер" — большое изображение, где 1 пиксель = величине
 * шага сетки визуализатора (scale). Пиксель (0,0) постера попадает на лист
 * (0,0) в точку печатной области (PRINTABLE_MM, PRINTABLE_MM) от края листа.
 */
public final class TilingPlan {

    /** Ширина листа A4, мм. */
    public static final double A4_WIDTH_MM = 210.0;
    /** Высота листа A4, мм. */
    public static final double A4_HEIGHT_MM = 297.0;
    /** Неизвлекаемое поле принтера с каждой стороны, мм (ТЗ: 3..10 мм). */
    public static final double PRINTER_UNPRINTABLE_MM = 5.0;
    /** Поле склейки (перекрытие соседних листов), мм (ТЗ рекомендует 5-10). */
    public static final double OVERLAP_MM = 10.0;
    /** Отступ начала печатной области от края листа, мм. */
    public static final double PRINTABLE_MM = PRINTER_UNPRINTABLE_MM + OVERLAP_MM; // 15

    private final int wPx, hPx, dpi;
    private final double stepXmm, stepYmm;
    private final int sheetsWide, sheetsHigh;

    private TilingPlan(int wPx, int hPx, int dpi) {
        this.wPx = wPx;
        this.hPx = hPx;
        this.dpi = dpi;
        this.stepXmm = A4_WIDTH_MM - 2 * PRINTABLE_MM; // 180
        this.stepYmm = A4_HEIGHT_MM - 2 * PRINTABLE_MM; // 267
        double stepXpx = mmToPx(stepXmm, dpi);
        double stepYpx = mmToPx(stepYmm, dpi);
        this.sheetsWide = Math.max(1, (int) Math.ceil(wPx / stepXpx));
        this.sheetsHigh = Math.max(1, (int) Math.ceil(hPx / stepYpx));
    }

    /** Миллиметры в пиксели при заданном DPI. */
    public static double mmToPx(double mm, int dpi) {
        return mm / 25.4 * dpi;
    }

    /** Пиксели в миллиметры при заданном DPI. */
    public static double pxToMm(double px, int dpi) {
        return px * 25.4 / dpi;
    }

    /**
     * Строит план по размеру изображения постера.
     *
     * @param wPx ширина постера, пикселей (&gt;0)
     * @param hPx высота постера, пикселей (&gt;0)
     * @param dpi разрешение сборки, &gt;0 (ТЗ: не ниже 300)
     */
    public static TilingPlan forImage(int wPx, int hPx, int dpi) {
        if (wPx <= 0 || hPx <= 0) {
            throw new IllegalArgumentException("Размеры изображения должны быть положительными");
        }
        if (dpi <= 0) {
            throw new IllegalArgumentException("DPI должен быть положительным");
        }
        return new TilingPlan(wPx, hPx, dpi);
    }

    public int sheetsWide() { return sheetsWide; }

    public int sheetsHigh() { return sheetsHigh; }

    public int totalSheets() { return sheetsWide * sheetsHigh; }

    public int dpi() { return dpi; }

    /** Шаг сетки листов по X, мм (полезная область минус перекрытие дважды? нет: шаг=ширина полезной области). */
    public double stepXmm() { return stepXmm; }

    /** Шаг сетки листов по Y, мм. */
    public double stepYmm() { return stepYmm; }

    /**
     * Позиция пикселя постера srcX на листе col в системе листа, мм от левого края листа.
     * srcX = 0 соответствует x = PRINTABLE_MM на любом листе (перекрытие листов).
     */
    public double sheetXMm(int col, double srcX) {
        return PRINTABLE_MM + (srcX - col * mmToPx(stepXmm, dpi));
    }

    /** Позиция пикселя постера srcY на листе row, мм от верхнего края листа. */
    public double sheetYMm(int row, double srcY) {
        return PRINTABLE_MM + (srcY - row * mmToPx(stepYmm, dpi));
    }

    /** Левый край области постера (в пикселях постера), попадающей на лист col. */
    public double originXpx(int col) {
        return col * mmToPx(stepXmm, dpi);
    }

    /** Верхний край области постера (в пикселях постера), попадающей на лист row. */
    public double originYpx(int row) {
        return row * mmToPx(stepYmm, dpi);
    }

    /** Размер листа A4 в пикселях при dpi плана, ширина. */
    public int sheetWidthPx() { return (int) Math.round(mmToPx(A4_WIDTH_MM, dpi)); }

    /** Размер листа A4 в пикселях при dpi плана, высота. */
    public int sheetHeightPx() { return (int) Math.round(mmToPx(A4_HEIGHT_MM, dpi)); }

    /** Физический размер собранного постера, мм. */
    public double posterWidthMm() { return pxToMm(wPx, dpi); }

    /** Физический размер собранного постера, мм. */
    public double posterHeightMm() { return pxToMm(hPx, dpi); }
}
