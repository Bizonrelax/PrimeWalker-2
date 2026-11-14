package io.github.bizonrelax.primewalker2.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты модели визуализаций (фрагмент 002, критерий готовности: VisualizerTest). */
class VisualizerTest {

    @Test
    void primesMatchesKnownValues() {
        assertTrue(Primes.isPrime(2));
        assertTrue(Primes.isPrime(97));
        assertFalse(Primes.isPrime(1));
        assertFalse(Primes.isPrime(100));
        assertTrue(Primes.isPrime(1_000_003L));
    }

    @Test
    void ulamSpiralStartsAtCenterAndCountMatchesSteps() {
        var v = new UlamSpiralVisualizer();
        var r = v.render(1, 10, 1.0);
        assertEquals(10, r.size());
        assertEquals(0.0, r.xs()[0]);
        assertEquals(0.0, r.ys()[0]);
        // против часовой: шаг 1 -> вправо? ТЗ: спираль Улама; проверяем соседство единиц расстоянием 1
        assertEquals(1.0, Math.abs(r.xs()[1] - r.xs()[0]) + Math.abs(r.ys()[1] - r.ys()[0]), 1e-9);
        assertEquals(1, r.values()[0]);
        assertEquals(10, r.values()[9]);
        assertTrue(r.primeFlags()[1]); // число 2 — простое
    }

    @Test
    void jacobsLadderFlipsDirectionOnPrimes() {
        var v = new JacobsLadderVisualizer();
        var r = v.render(2, 6, 1.0); // 2P 3P 4 5P 6 7P; стартовое направление — вверх (ТЗ)
        // y фиксируется до смены направления; scale=1.0 для шага лестницы:
        assertEquals(0.0, r.ys()[0]);   // 2: старт, простое -> разворот вниз
        assertEquals(1.0, r.ys()[1]);   // 3: шаг вверх, простое -> разворот вниз
        assertEquals(-1.0, r.ys()[2]);  // 4: вниз, не простое
        assertEquals(-2.0, r.ys()[3]);  // 5: вниз, простое -> разворот вверх
        assertEquals(-1.0, r.ys()[4]);  // 6: вверх
        assertEquals(0.0, r.ys()[5]);   // 7: вверх, простое -> разворот
    }

    @Test
    void primeWalkStepByLastDigit() {
        assertArrayEquals(new int[]{-1, 0}, PrimeWalkVisualizer.stepFor(11)); // ...1 влево
        assertArrayEquals(new int[]{1, 0}, PrimeWalkVisualizer.stepFor(13));  // ...3 вправо
        assertArrayEquals(new int[]{0, 0}, PrimeWalkVisualizer.stepFor(4));   // не простое — стоя
        assertArrayEquals(new int[]{0, 0}, PrimeWalkVisualizer.stepFor(2));
        var r = new PrimeWalkVisualizer().render(11, 3, 1.0); // 11,12,13
        assertEquals(0.0, r.xs()[0]);
        assertEquals(-1.0, r.xs()[1]); // после 11 сдвиг влево
        assertEquals(-1.0, r.xs()[2]); // 12 не простое — без сдвига
    }

    @Test
    void boundsCoverAllPoints() {
        var r = new UlamSpiralVisualizer().render(1, 50, 2.0);
        for (int i = 0; i < r.size(); i++) {
            assertTrue(r.xs()[i] >= r.minX() - 1e-9 && r.xs()[i] <= r.maxX() + 1e-9);
            assertTrue(r.ys()[i] >= r.minY() - 1e-9 && r.ys()[i] <= r.maxY() + 1e-9);
        }
        assertEquals(50, r.path().size());
    }

    @Test
    void colorModuloSchemeIndexInRange() {
        assertEquals(0, ColorModuloScheme.colorIndexFor(12, 3));
        assertEquals(2, ColorModuloScheme.colorIndexFor(-1, 3)); // floorMod
        for (long n : new long[]{-7, 0, 7, 11, 99}) {
            int idx = ColorModuloScheme.colorIndexFor(n, 6);
            assertTrue(idx >= 0 && idx < 6);
        }
        assertThrows(IllegalArgumentException.class, () -> ColorModuloScheme.colorIndexFor(5, 0));
    }

    @Test
    void factoryListsThreeVisualizersAndByName() {
        assertEquals(3, VisualizerFactory.all().size());
        assertEquals("Ulam Spiral", VisualizerFactory.byName("ulam spiral").displayName());
        assertEquals("Prime Walk", VisualizerFactory.byName("Prime Walk").displayName());
        assertEquals("Ulam Spiral", VisualizerFactory.byName("нет такого").displayName()); // default по ТЗ
    }

    @Test
    void renderRejectsNonPositiveSteps() {
        assertThrows(IllegalArgumentException.class, () -> new UlamSpiralVisualizer().render(1, 0, 1.0));
    }
}
