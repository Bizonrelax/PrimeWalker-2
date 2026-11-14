package io.github.bizonrelax.primewalker2.prime;

/**
 * Стратегия получения простых чисел до limit включительно (TZ: выбор алгоритма).
 */
public interface PrimeGenerationStrategy {
    long[] generate(long limit);

    String name();
}
