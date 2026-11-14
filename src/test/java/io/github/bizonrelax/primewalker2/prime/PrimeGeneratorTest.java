package io.github.bizonrelax.primewalker2.prime;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PrimeGeneratorTest {

    private static final long[] FIRST = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31};

    @Test
    void eratosthenesMatchesKnownPrimes() {
        assertArrayEquals(FIRST, new EratosthenesSieve().generate(31));
        assertEquals(25, new EratosthenesSieve().generate(100).length); // π(100)=25
    }

    @Test
    void segmentedMatchesEratosthenes() {
        for (long lim : new long[]{2, 3, 10, 97, 65536, 100_003}) {
            assertArrayEquals(new EratosthenesSieve().generate(lim),
                    new SegmentedSieve().generate(lim), "limit=" + lim);
        }
        assertEquals(41538, new SegmentedSieve().generate(500_000).length); // π(5e5)
    }

    @Test
    void trialDivisionMatchesEratosthenes() {
        assertArrayEquals(new EratosthenesSieve().generate(10_000),
                new TrialDivision().generate(10_000));
    }

    @Test
    void smallAndEmptyLimits() {
        for (PrimeGenerationStrategy a : new PrimeGenerationStrategy[]{
                new EratosthenesSieve(), new SegmentedSieve(), new TrialDivision()}) {
            assertEquals(0, a.generate(1).length, a.name());
            assertEquals(1, a.generate(2).length, a.name());
            assertEquals(0, a.generate(0).length, a.name());
        }
    }

    @Test
    void sortedAndUnique() {
        long[] p = PrimeGenerator.primes(100_000, PrimeGenerator.byName(PrimeGenerator.SEGMENTED));
        for (int i = 1; i < p.length; i++) assertTrue(p[i] > p[i - 1], "not sorted at " + i);
        assertTrue(Arrays.stream(p).allMatch(n -> n >= 2));
    }

    @Test
    void maxStepsCapApplied() {
        assertEquals(PrimeGenerator.MAX_STEPS, PrimeGenerator.primes(Long.MAX_VALUE >> 1,
                new PrimeGenerationStrategy() {
                    @Override public long[] generate(long limit) { return new long[]{limit}; }
                    @Override public String name() { return "probe"; }
                })[0]);
        assertEquals(100_000_000L, PrimeGenerator.MAX_STEPS);
    }

    @Test
    void byNameResolvesAllAlgorithms() {
        assertEquals("Eratosthenes", PrimeGenerator.byName(PrimeGenerator.ERATOSTHENES).name());
        assertEquals("Segmented", PrimeGenerator.byName(PrimeGenerator.SEGMENTED).name());
        assertEquals("TrialDivision", PrimeGenerator.byName(PrimeGenerator.TRIAL_DIVISION).name());
        assertEquals("Eratosthenes", PrimeGenerator.byName(null).name());
        assertThrows(IllegalArgumentException.class, () -> PrimeGenerator.byName("Nope"));
        assertThrows(IllegalArgumentException.class, () -> PrimeGenerator.primes(-1));
    }

    @Test
    void isPrimeEdgeCases() {
        assertFalse(TrialDivision.isPrime(1));
        assertTrue(TrialDivision.isPrime(2));
        assertFalse(TrialDivision.isPrime(9));
        assertTrue(TrialDivision.isPrime(7919));
        assertTrue(TrialDivision.isPrime(1_000_000_007L));
    }
}
