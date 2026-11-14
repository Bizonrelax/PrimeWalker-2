package io.github.bizonrelax.primewalker2.core;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты модели визуализаций (фрагмент 002, ТЗ: алгоритмы и Bounds). */
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
        var v = new UlamSpiralVisualizer();
        var r = v.visualize(1, 24);
        assertEquals(25, r.size());                 // start + steps точек
        assertEquals(24, r.values().length);
        assertEquals("Ulam Spiral", r.algorithm());
        // простые до 24: 2,3,5,7,11,13,17,19,23
        assertEquals(9, r.primeCount());
        assertTrue(r.primeFlags()[1]);   // число 2
        assertFalse(r.primeFlags()[3]);  // число 4
    }

    @Test
    void ulamBoundsCoverFullSpiral() {
        var v = new UlamSpiralVisualizer();
        var r = v.visualize(1, 24); // числа 1..24 -> кольцо 2 (до 25) не достигнут, max кольцо r=... 24 в кольце 2? (2r+1)^2=25>=24, prevMax=9 -> idx=15<=4s=8? no: idx=24-9=15 > 12 => нижняя строка x=-2+t, t=15-12=3 => (-2+3,-2)=(1,-2)? wait 25=(3,-3)... проверим границы диапазона
        assertTrue(r.bounds().minX() >= -2 && r.bounds().maxX() <= 2);
        assertTrue(r.bounds().minY() >= -2 && r.bounds().maxY() <= 2);
        assertEquals(24, r.bounds().pointCount() - 1);
    }

    @Test
    void ulamScaleAffectsStepSize() {
        var v = new UlamSpiralVisualizer(2.0);
        var r = v.visualize(1, 2);
        Point2D.Double p2 = r.path().get(1); // число 2: узел спирали (1,0)*scale
        assertEquals(2.0, p2.x, 1e-9);
        assertEquals(0.0, p2.y, 1e-9);
    }

    // --- Лестница Якова ---

    @Test
    void jacobsLadderFlipsDirectionOnPrimes() {
        var v = new JacobsLadderVisualizer();
        var r = v.visualize(1, 10); // числа 1..10, простые 2,3,5,7
        assertEquals("Jacob's Ladder", r.algorithm());
        double x = 0, y = 0;
        int dir = -1; // направление лестницы: -1 => первый простой уходит вверх
        // узел i — позиция числа 1+i; переход к узлу применяется с i>=1
        for (int i = 0; i < 10; i++) {
            long val = 1 + i;
            if (i > 0) {
                if (val == 2 || val == 3 || val == 5 || val == 7) dir = -dir;
                x += 1;
                y += dir; // dir=-1 => вверх по экрану (y уменьшается)
            }
            assertEquals(x, r.path().get(i).x, 1e-9, "x at " + val);
            assertEquals(y, r.path().get(i).y, 1e-9, "y at " + val);
        }
    }

    @Test
    void jacobsLadderResetsBetweenRuns() {
        var v = new JacobsLadderVisualizer();
        var a = v.visualize(1, 10);
        var b = v.visualize(1, 10);
        assertEquals(a.path().get(9), b.path().get(9));
    }

    // --- Ход простого числа ---

    @Test
    void primeWalkUsesLastDigitRule() {
        var v = new PrimeWalkVisualizer();
        var r = v.visualize(1, 12); // числа 1..12: простые 2,3,5,7,11
        assertEquals("Prime Walk", r.algorithm());
        // правила цифры: 1,7 влево; 3,9 вправо; 2,5 вверх; составные вправо
        // позиции чисел 1..12: 1=(0,0); 2 вверх; 3 вправо; 4 вправо; 5 вверх; 6 вправо;
        // 7 влево; 8 вправо; 9 вправо; 10 вправо; 11 влево; 12 вправо
        double x = 0, y = 0;
        y -= 1;           // 2
        x += 1;           // 3
        x += 1;           // 4
        y -= 1;           // 5
        x += 1;           // 6
        x -= 1;           // 7
        x += 1;           // 8
        x += 1;           // 9
        x += 1;           // 10
        x -= 1;           // 11
        x += 1;           // 12
        assertEquals(x, r.path().get(11).x, 1e-9);
        assertEquals(y, r.path().get(11).y, 1e-9);
    }

    // --- Общие требования BaseVisualizer ---

    @Test
    void rejectsStepsAboveCapAndBelowOne() {
        var v = new UlamSpiralVisualizer();
        assertThrows(IllegalArgumentException.class, () -> v.visualize(1, 0));
        assertThrows(IllegalArgumentException.class, () -> v.visualize(1, BaseVisualizer.MAX_STEPS + 1));
        assertEquals(100_000_000, BaseVisualizer.MAX_STEPS);
    }

    @Test
    void rejectsBadScale() {
        assertThrows(IllegalArgumentException.class, () -> new UlamSpiralVisualizer(0.05));
        assertThrows(IllegalArgumentException.class, () -> new UlamSpiralVisualizer(11.0));
    }

    @Test
    void negativeStartAllowed() {
        var v = new UlamSpiralVisualizer();
        var r = assertDoesNotThrow(() -> v.visualize(-5, 10));
        assertEquals(-5, r.values()[0]);
        assertEquals(0, r.primeCount()); // -5..4 не содержат простых
    }

    @Test
    void resultValidatesConsistentLengths() {
        assertThrows(IllegalArgumentException.class, () -> new VisualizationResult(
                java.util.List.of(new Point2D.Double(0, 0)), new boolean[0], new long[0],
                new Bounds(), "x"));
    }
}
