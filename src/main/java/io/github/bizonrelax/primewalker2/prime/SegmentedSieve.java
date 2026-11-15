package io.github.bizonrelax.primewalker2.prime;

/**
 * Сегментное решето: мелкие простые задают базис, крупные собираются сегментами.
 * Позволяет доходить до MAX_STEPS при умеренном расходе памяти.
 */
public final class SegmentedSieve implements PrimeGenerationStrategy {
    @Override
    public long[] generate(long limit) {
        if (limit < 2) return new long[0];
        long sq = (long) Math.sqrt(limit);
        while ((sq + 1) * (sq + 1) <= limit) sq++;
        while (sq * sq > limit) sq--;
        long[] small = new EratosthenesSieve().generate(sq);
        int segSize = 1 << 16;
        long[] big = new long[segSize];
        int cnt = 0;
        for (long lo = sq + 1; lo <= limit; lo += segSize) {
            long hi = Math.min(lo + segSize - 1, limit);
            boolean[] mark = new boolean[(int) (hi - lo + 1)];
            for (long p : small) {
                long start = (lo + p - 1) / p * p;
                for (long m = start; m <= hi; m += p) mark[(int) (m - lo)] = true;
            }
            for (int i = 0; i < mark.length; i++) {
                if (!mark[i]) {
                    if (cnt == big.length) {
                        long[] g = new long[big.length * 2];
                        System.arraycopy(big, 0, g, 0, big.length);
                        big = g;
                    }
                    big[cnt++] = lo + i;
                }
            }
        }
        long[] r = new long[small.length + cnt];
        System.arraycopy(small, 0, r, 0, small.length);
        System.arraycopy(big, 0, r, small.length, cnt);
        return r;
    }

    @Override
    public String name() { return "Segmented"; }
}
