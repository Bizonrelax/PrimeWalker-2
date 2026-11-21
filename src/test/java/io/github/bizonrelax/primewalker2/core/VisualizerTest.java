package io.github.bizonrelax.primewalker2.core;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты модели визуализаций (фрагмент 002, критерий готовности: VisualizerTest). */
class VisualizerTest {

    // --- Спираль Улама ---

    @Test
    void spiralPositionsMatchCounterClockwiseLayout() {
        assertArrayEquals(new long[]{0, 0}, UlamSpiralVisualizer.spiralPos(1));
        assertArrayEquals(new long[]{1, 0}, UlamSpiralVisualizer.spiralPos(2));
        assertArrayEquals(new long[]{1, 1}, UlamSpiralVisualizer.spiralPos(3));
        assertArrayEquals(new long[]{-1, 1}, UlamSpiralVisualizer.spiralPos(5));
        assertArrayEquals(new long[]{-1, -1}, UlamSpiralVisualizer.spiralPos(7));
        assertArrayEquals(new long[]{1, -1}, UlamSpiralVisualizer.spiralPos(9));
        assertArrayEquals(new long[]{2, -1}, UlamSpiralVisualizer.spiralPos(10));
        assertArrayEquals(new long[]{2, 2}, UlamSpiralVisualizer.spiralPos(13));
        assertArrayEquals(new long[]{-2, 2}, UlamSpiralVisualizer.spiralPos(17));
        assertArrayEquals(new long[]{2, -2}, UlamSpiralVisualizer.spiralPos(25));
    }

    @Test
    void ulamPathLengthAndFlags() {
        var r = new UlamSpiralVisualizer().render(1, 24, 1.0);
        assertEquals(24, r.size());
        assertEquals(24, r.values().length);
        assertEquals("Ulam Spiral", r.algorithm());
        assertEquals(9, r.primeCount());          // 2,3,5,7,11,13,17,19,23
        assertFalse(r.primeFlags()[0]);           // число 1 — не простое
        assertTrue(r.primeFlags()[1]);            // число 2
        assertFalse(r.primeFlags()[3]);           // число 4
        assertEquals(0.0, r.path().get(0).x, 1e-9); // первое число в центре
    }

    @Test
    void ulamBoundsCoverFullSpiral() {
        var r = new UlamSpiralVisualizer().render(1, 24, 1.0);
        // числа 1..24 лежат внутри кольца радиуса 2 ((2*2+1)^2 = 25 > 24)
        assertTrue(r.bounds().minX() >= -2 && r.bounds().maxX() <= 2);
        assertTrue(r.bounds().minY() >= -2 && r.bounds().maxY() <= 2);
        assertEquals(24, r.bounds().pointCount());
    }

    @Test
    void ulamScaleAffectsStepSize() {
        var r = new UlamSpiralVisualizer().render(1, 2, 2.0);
        Point2D.Double p2 = r.path().get(1); // число 2: узел спирали (1,0)*scale
        assertEquals(2.0, p2.x, 1e-9);
        assertEquals(0.0, p2.y, 1e-9);
    }

    // --- Лестница Якова ---

    @Test
    void jacobsLadderFlipsDirectionOnPrimes() {
        var v = new JacobsLadderVisualizer();
        var r = v.render(2, 6, 1.0); // 2P 3P 4 5P 6 7P; старт направления — вверх
        assertEquals("Jacob's Ladder", r.algorithm());
        // ТЗ: направление меняется при встрече простого; позиция i = i-й переход из точки 2
        double[] expectedY = {0, 1, 0, -1, 0, 1};
        for (int i = 0; i < 6; i++) {
            assertEquals(i, r.path().get(i).x, 1e-9, "x at step " + i);
            assertEquals(expectedY[i], r.path().get(i).y, 1e-9, "y at step " + i);
        }
    }

    @Test
    void jacobsLadderDeterministicBetweenRuns() {
        var v = new JacobsLadderVisualizer();
        var a = v.render(1, 10, 1.0);
        var b = v.render(1, 10, 1.0);
        assertEquals(a.path().get(9), b.path().get(9));
    }

    // --- Ход простого числа ---

    @Test
    void primeWalkUsesLastDigitRule() {
        assertArrayEquals(new int[]{-1, 0}, PrimeWalkVisualizer.stepFor(11)); // ...1 влево
        assertArrayEquals(new int[]{0, 1}, PrimeWalkVisualizer.stepFor(13));  // ...3 вверх (ТЗ рев.2)
        assertArrayEquals(new int[]{1, 0}, PrimeWalkVisualizer.stepFor(17));  // ...7 вправо (ТЗ рев.2)
        assertArrayEquals(new int[]{0, -1}, PrimeWalkVisualizer.stepFor(19)); // ...9 вниз (ТЗ рев.2)
        assertArrayEquals(new int[]{0, 0}, PrimeWalkVisualizer.stepFor(4));   // составное — стоя
        assertArrayEquals(new int[]{0, 0}, PrimeWalkVisualizer.stepFor(2));   // 2 игнорируется ТЗ
        assertArrayEquals(new int[]{0, 0}, PrimeWalkVisualizer.stepFor(5));   // 5 игнорируется ТЗ
        var r = new PrimeWalkVisualizer().render(11, 3, 1.0); // 11,12,13
        assertEquals(0.0, r.path().get(0).x, 1e-9);
        assertEquals(-1.0, r.path().get(1).x, 1e-9); // после 11 сдвиг влево
        assertEquals(-1.0, r.path().get(2).x, 1e-9); // 12 составное — без сдвига
        assertEquals(0.0, r.path().get(2).y, 1e-9);
    }

    @Test
    void primeWalkCompositeDoNotMove() {
        var r = new PrimeWalkVisualizer().render(24, 3, 1.0); // 24,25,26 — все составные
        assertEquals(0.0, r.path().get(2).x, 1e-9);
        assertEquals(0.0, r.path().get(2).y, 1e-9);
    }

    @Test
    void primeWalkIsTwoDimensionalNotAStrip() {
        // ТЗ рев.2 (CHANGE-001): трасса не должна вырождаться в узкую горизонтальную полосу
        var r = new PrimeWalkVisualizer().render(2, 5000, 1.0);
        double spanX = r.maxX() - r.minX();
        double spanY = r.maxY() - r.minY();
        assertTrue(spanX > 10, "размах по X: " + spanX);
        assertTrue(spanY > 10, "размах по Y: " + spanY);
        assertTrue(Math.min(spanX, spanY) / Math.max(spanX, spanY) > 0.1,
                "трасса вырождена в полосу: " + spanX + "x" + spanY);
    }

    // --- Общие требования BaseVisualizer ---

    @Test
    void rejectsStepsAboveCapAndBelowOne() {
        var v = new UlamSpiralVisualizer();
        assertThrows(IllegalArgumentException.class, () -> v.render(1, 0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> v.render(1, -3, 1.0));
        assertThrows(IllegalArgumentException.class, () -> v.render(1, BaseVisualizer.MAX_STEPS + 1, 1.0));
        assertEquals(100_000_000, BaseVisualizer.MAX_STEPS);
    }

    @Test
    void rejectsBadScale() {
        var v = new UlamSpiralVisualizer();
        assertThrows(IllegalArgumentException.class, () -> v.render(1, 2, 0.05));
        assertThrows(IllegalArgumentException.class, () -> v.render(1, 2, 11.0));
    }

    @Test
    void negativeStartAllowed() {
        var r = assertDoesNotThrow(() -> new UlamSpiralVisualizer().render(-5, 10, 1.0));
        assertEquals(-5, r.values()[0]);
        assertEquals(2, r.primeCount()); // -5..4 содержат простые 2 и 3
    }

    @Test
    void boundsAccumulateMinMax() {
        var b = new Bounds();
        assertTrue(b.isEmpty());
        assertEquals(0.0, b.minX(), 1e-9);
        b.extend(2, -3);
        b.extend(-1, 5);
        assertFalse(b.isEmpty());
        assertEquals(-1, b.minX(), 1e-9);
        assertEquals(2, b.maxX(), 1e-9);
        assertEquals(-3, b.minY(), 1e-9);
        assertEquals(5, b.maxY(), 1e-9);
        assertEquals(3, b.width(), 1e-9);
        assertEquals(8, b.height(), 1e-9);
        assertEquals(2, b.pointCount());
    }

    @Test
    void resultValidatesConsistentLengths() {
        var one = java.util.List.of(new Point2D.Double(0, 0));
        assertDoesNotThrow(() -> new VisualizationResult(one, new boolean[1], new long[1], new Bounds(), "x"));
        assertThrows(IllegalArgumentException.class, () -> new VisualizationResult(
                java.util.List.of(), new boolean[0], new long[0], new Bounds(), "x"));
        assertThrows(IllegalArgumentException.class, () -> new VisualizationResult(
                java.util.List.of(new Point2D.Double(0, 0)), new boolean[0], new long[0], new Bounds(), "x"));
        assertThrows(IllegalArgumentException.class, () -> new VisualizationResult(
                java.util.List.of(new Point2D.Double(0, 0)), new boolean[1], new long[2], new Bounds(), "x"));
        assertThrows(NullPointerException.class, () -> new VisualizationResult(
                null, new boolean[1], new long[1], new Bounds(), "x"));
    }

    @Test
    void convenienceBoundsAccessorsMatchBounds() {
        var r = new UlamSpiralVisualizer().render(1, 10, 1.0);
        assertEquals(r.bounds().minX(), r.minX(), 1e-9);
        assertEquals(r.bounds().maxY(), r.maxY(), 1e-9);
    }

    // --- Цветовая кодировка по модулю N (ТЗ, алгоритм 4) ---

    @Test
    void colorModuloSchemeIndexInRange() {
        assertEquals(0, ColorModuloScheme.colorIndexFor(12, 3));
        assertEquals(2, ColorModuloScheme.colorIndexFor(-1, 3)); // floorMod
        for (long n : new long[]{-7, 0, 7, 11, 99}) {
            for (int m = 3; m <= 8; m++) {
                int idx = ColorModuloScheme.colorIndexFor(n, m);
                assertTrue(idx >= 0 && idx < m, "idx=" + idx + " n=" + n + " m=" + m);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> ColorModuloScheme.colorIndexFor(5, 0));
    }

    // --- Фабрика визуализаций ---

    @Test
    void factoryListsThreeVisualizersAndByName() {
        assertEquals(3, VisualizerFactory.all().size());
        assertEquals("Ulam Spiral", VisualizerFactory.byName("ulam spiral").displayName());
        assertEquals("Prime Walk", VisualizerFactory.byName("Prime Walk").displayName());
        assertEquals("Jacob's Ladder", VisualizerFactory.byName("jacob's ladder").displayName());
        assertEquals("Ulam Spiral", VisualizerFactory.byName("нет такого").displayName()); // default по ТЗ
    }

    // --- Детерминизм флагов (арбитр — ТЗ, независимо от кэша prime.*) ---

    @Test
    void flagsMatchTrialDivisionForRange() {
        var r = new UlamSpiralVisualizer().render(90, 40, 1.0);
        int known = 0;
        for (long n = 90; n < 130; n++) {
            boolean exp = n > 1;
            for (long d = 2; d * d <= n && exp; d++) exp = n % d != 0;
            assertEquals(exp, r.primeFlags()[(int) (n - 90)], "n=" + n);
            if (exp) known++;
        }
        assertEquals(known, r.primeCount());
    }
}
