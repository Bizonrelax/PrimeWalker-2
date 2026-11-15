package io.github.bizonrelax.primewalker2.prime;

import java.util.Arrays;

/** Классическое решето Эратосфена, память O(n) байт. */
public final class EratosthenesSieve implements PrimeGenerationStrategy {
    @Override
    public long[] generate(long limit) {
        if (limit < 2) return new long[0];
        int n = (int) limit;
        boolean[] comp = new boolean[n + 1];
        for (int p = 2; (long) p * p <= n; p++) {
            if (!comp[p]) {
                for (int m = p * p; m <= n; m += p) comp[m] = true;
            }
        }
        int cnt = 0;
        for (int i = 2; i <= n; i++) if (!comp[i]) cnt++;
        long[] r = new long[cnt];
        int k = 0;
        for (int i = 2; i <= n; i++) if (!comp[i]) r[k++] = i;
        return r;
    }

    @Override
    public String name() { return "Eratosthenes"; }

    @Override
    public boolean equals(Object o) { return o instanceof EratosthenesSieve; }

    @Override
    public int hashCode() { return Arrays.hashCode(new int[]{1}); }
}
