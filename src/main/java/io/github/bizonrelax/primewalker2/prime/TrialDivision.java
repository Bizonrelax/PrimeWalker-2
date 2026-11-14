package io.github.bizonrelax.primewalker2.prime;

/** Пробное деление нечётными делителями до √n. */
public final class TrialDivision implements PrimeGenerationStrategy {
    @Override
    public long[] generate(long limit) {
        if (limit < 2) return new long[0];
        int cnt = 0;
        long[] tmp = new long[1024];
        for (long n = 2; n <= limit; n++) {
            if (isPrime(n)) {
                if (cnt == tmp.length) {
                    long[] g = new long[tmp.length * 2];
                    System.arraycopy(tmp, 0, g, 0, tmp.length);
                    tmp = g;
                }
                tmp[cnt++] = n;
            }
        }
        long[] r = new long[cnt];
        System.arraycopy(tmp, 0, r, 0, cnt);
        return r;
    }

    static boolean isPrime(long n) {
        if (n < 2) return false;
        if (n % 2 == 0) return n == 2;
        if (n % 3 == 0) return n == 3;
        for (long d = 5; d * d <= n; d += 6) {
            if (n % d == 0 || n % (d + 2) == 0) return false;
        }
        return true;
    }

    @Override
    public String name() { return "TrialDivision"; }
}
