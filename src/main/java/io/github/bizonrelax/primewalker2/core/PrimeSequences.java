package io.github.bizonrelax.primewalker2.core;

/**
 * Детерминированная проверка простоты для модели визуализаций (ТЗ: арбитр — ТЗ,
 * поведение не должно зависеть от состояния кэша генератора prime.*).
 */
final class PrimeSequences {

    private PrimeSequences() {
    }

    /** Истинно, если value >= 2 и делится только на 1 и себя. */
    static boolean isPrime(long value) {
        if (value < 2) return false;
        if (value % 2 == 0) return value == 2;
        for (long d = 3; d * d <= value; d += 2) {
            if (value % d == 0) return false;
        }
        return true;
    }
}
