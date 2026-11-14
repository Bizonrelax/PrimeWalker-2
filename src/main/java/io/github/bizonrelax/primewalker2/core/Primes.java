package io.github.bizonrelax.primewalker2.core;

import java.math.BigInteger;

/** Проверка простоты через BigInteger.isPrime — без зависимости от пакета prime. */
public final class Primes {
    private Primes() {}

    public static boolean isPrime(long n) {
        return n >= 0 && BigInteger.valueOf(n).isProbablePrime(20);
    }
}
