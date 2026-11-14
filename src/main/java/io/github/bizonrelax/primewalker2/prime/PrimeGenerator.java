package io.github.bizonrelax.primewalker2.prime;

import java.util.Objects;

/**
 * Фасад генерации простых чисел: выбор алгоритма по имени и ограничение шагов
 * (TZ: количество шагов не более MAX_STEPS).
 */
public final class PrimeGenerator {
    public static final long MAX_STEPS = 100_000_000L;
    public static final String ERATOSTHENES = "Eratosthenes";
    public static final String SEGMENTED = "Segmented";
    public static final String TRIAL_DIVISION = "TrialDivision";

    private PrimeGenerator() {}

    public static PrimeGenerationStrategy byName(String name) {
        if (name == null) return new EratosthenesSieve();
        switch (name) {
            case SEGMENTED: return new SegmentedSieve();
            case TRIAL_DIVISION: return new TrialDivision();
            case ERATOSTHENES: return new EratosthenesSieve();
            default: throw new IllegalArgumentException("Unknown algorithm: " + name);
        }
    }

    /** Простые до steps включительно; steps ограничен сверху MAX_STEPS. */
    public static long[] primes(long steps, PrimeGenerationStrategy alg) {
        Objects.requireNonNull(alg, "alg");
        if (steps < 0) throw new IllegalArgumentException("steps < 0");
        long lim = Math.min(steps, MAX_STEPS);
        return alg.generate(lim);
    }

    public static long[] primes(long steps) {
        return primes(steps, byName(null));
    }

    public static String[] algorithmNames() {
        return new String[]{ERATOSTHENES, SEGMENTED, TRIAL_DIVISION};
    }
}
