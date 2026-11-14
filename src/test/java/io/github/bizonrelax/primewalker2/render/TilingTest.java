package io.github.bizonrelax.primewalker2.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты плана тесселяции A4 (фрагмент 003). */
class TilingTest {

    /** Шаг сетки листов по X, мм (поле склейки + неизвлекаемое поле). */
    private static final double STEP_X_MM = TilingPlan.STEP_X_MM; // 195
    /** Шаг сетки листов по Y, мм. */
    private static final double STEP_Y_MM = TilingPlan.STEP_Y_MM; // 282

    @Test
    void smallImageFitsOneSheet() {
        int dpi = 300;
        int w = (int) TilingPlan.mmToPx(150, dpi);
        int h = (int) TilingPlan.mmToPx(200, dpi);
        TilingPlan plan = TilingPlan.forImage(w, h, dpi);
        assertEquals(1, plan.sheetsWide());
        assertEquals(1, plan.sheetsHigh());
        assertEquals(1, plan.totalSheets());
    }

    @Test
    void computesGridWithOverlap() {
        int dpi = 300;
        // ровно 4 шага по X -> 4 колонки; чуть больше 1 шага по Y -> 2 ряда
        int w = (int) Math.floor(TilingPlan.mmToPx(4 * STEP_X_MM, dpi));
        int h = (int) Math.ceil(TilingPlan.mmToPx(STEP_Y_MM + 1, dpi));
        TilingPlan plan = TilingPlan.forImage(w, h, dpi);
        assertEquals(4, plan.sheetsWide());
        assertEquals(2, plan.sheetsHigh());
        assertEquals(8, plan.totalSheets());
    }

    @Test
    void exactlyFourStepsIsFourColumns() {
        int dpi = 300;
        int w = (int) Math.floor(TilingPlan.mmToPx(4 * STEP_X_MM, dpi));
        assertEquals(4, TilingPlan.forImage(w, 100, dpi).sheetsWide());
        assertEquals(5, TilingPlan.forImage(w + 2, 100, dpi).sheetsWide());
    }

    @Test
    void mmPxConversionRoundtrip() {
        assertEquals(210.0, TilingPlan.pxToMm(TilingPlan.mmToPx(210, 300), 300), 1e-9);
        assertEquals(2480, Math.round(TilingPlan.mmToPx(210, 300)), 2); // A4 ширина при 300dpi ~2480px
    }

    @Test
    void originOffsetsStepByPrintableArea() {
        TilingPlan plan = TilingPlan.forImage(10000, 10000, 300);
        double stepX = TilingPlan.mmToPx(STEP_X_MM, 300);
        double stepY = TilingPlan.mmToPx(STEP_Y_MM, 300);
        assertEquals(0, plan.originXpx(0), 1e-9);
        assertEquals(stepX, plan.originXpx(1), 1e-6);
        assertEquals(2 * stepY, plan.originYpx(2), 1e-6);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> TilingPlan.forImage(0, 100, 300));
        assertThrows(IllegalArgumentException.class, () -> TilingPlan.forImage(100, 100, 0));
    }
}
