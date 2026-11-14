package io.github.bizonrelax.primewalker2.core;

/** Цветовая кодировка по модулю N (ТЗ, алгоритм 4): индекс палитры для остатка. */
public final class ColorModuloScheme {

    private ColorModuloScheme() {}

    /** Индекс цвета для числа n при модуле modulo (3..8 по ТЗ), всегда 0 <= index < modulo. */
    public static int colorIndexFor(long n, int modulo) {
        if (modulo <= 0) throw new IllegalArgumentException("modulo > 0");
        return (int) Math.floorMod(n, modulo);
    }
}
