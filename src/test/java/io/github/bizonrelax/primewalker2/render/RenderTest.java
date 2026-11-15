package io.github.bizonrelax.primewalker2.render;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты рендеринга трассы и листов (фрагмент 003). Headless-safe: только BufferedImage. */
class RenderTest {

    @Test
    void rendersPrimePointsAndBackground() {
        var svc = new RenderService();
        var opts = new RenderOptions(200, 200, 300, Color.WHITE,
                Color.RED, Color.BLUE, Color.GRAY, 0, 1, true);
        // квадратный контур против часовой: (0,0)->(10,0)->(10,10)->(0,10)
        var path = List.of(new Double(0, 0), new Double(10, 0),
                new Double(10, 10), new Double(0, 10));
        BufferedImage img = svc.renderImage(path,
                new boolean[]{true, true, false}, null,
                new Pose(15, 15, 10, 10), opts);
        assertEquals(200, img.getWidth());
        // фон белый в пустой области
        assertEquals(Color.WHITE.getRGB() & 0xFFFFFF, img.getRGB(5, 5) & 0xFFFFFF);
        // точка (10*15+10, 0*15+10)=(160,10) — простая, красная
        assertEquals(Color.RED.getRGB() & 0xFFFFFF, img.getRGB(160, 10) & 0xFFFFFF);
        // последняя точка (0,10)->(10,160) — синяя (endColor)
        assertEquals(Color.BLUE.getRGB() & 0xFFFFFF, img.getRGB(10, 160) & 0xFFFFFF);
    }

    @Test
    void moduloColorsAreDeterministic() {
        Color a = RenderService.moduloColor(7, 4);
        Color b = RenderService.moduloColor(11, 4); // оба остаток 3
        assertEquals(a, b);
        assertNotEquals(RenderService.moduloColor(8, 4), a);
        assertThrows(IllegalArgumentException.class, () -> RenderService.moduloColor(5, 0));
    }

    @Test
    void poseMapsGridToPixels() {
        Pose p = new Pose(2, 4, 10, 20);
        assertEquals(30, p.px(10));
        assertEquals(60, p.py(10));
        Pose t = p.translated(-100, -200);
        assertEquals(-70, t.px(10));
        assertEquals(-140, t.py(10));
        assertThrows(IllegalArgumentException.class, () -> new Pose(0, 1, 0, 0));
    }

    @Test
    void renderOptionsValidate() {
        assertThrows(IllegalArgumentException.class,
                () -> RenderOptions.defaults(0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new RenderOptions(100, 100, 300, Color.WHITE, Color.RED,
                        Color.BLUE, Color.GRAY, 99, 2, true));
        var d = RenderOptions.defaults(100, 100);
        assertEquals(300, d.dpi());
    }

    @Test
    void sheetHasCropMarksAndSize() {
        int dpi = 300;
        var plan = TilingPlan.forImage(
                (int) TilingPlan.mmToPx(400, dpi), (int) TilingPlan.mmToPx(400, dpi), dpi);
        assertEquals(3, plan.sheetsWide()); // 400/180 -> 3
        assertEquals(2, plan.sheetsHigh()); // 400/267 -> 2
        assertEquals(6, plan.totalSheets());
        var sr = new SheetRenderer();
        BufferedImage sheet = sr.renderSheet(List.of(new Double(500, 500)),
                new boolean[]{true}, new long[]{7}, plan, 0, 0, RenderOptions.defaults(100, 100));
        assertEquals(plan.sheetWidthPx(), sheet.getWidth());
        assertEquals(plan.sheetHeightPx(), sheet.getHeight());
        // crop mark: у левого края листа на линии реза x=5мм есть тёмный пиксель
        double pxPerMm = dpi / 25.4;
        int cutX = (int) Math.round(5 * pxPerMm);
        int midY = sheet.getHeight() / 2;
        boolean dark = false;
        for (int y = midY - 20; y < midY + 20; y++) {
            int rgb = sheet.getRGB(cutX, y) & 0xFFFFFF;
            int lum = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
            if (lum < 100) { dark = true; break; }
        }
        assertTrue(dark, "на линии реза должны быть монтажные метки");
        assertThrows(IllegalArgumentException.class, () -> sr.renderSheet(
                List.of(), new boolean[0], null, plan, 3, 0, RenderOptions.defaults(100, 100)));
    }
}
