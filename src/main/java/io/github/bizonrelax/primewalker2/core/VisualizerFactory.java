package io.github.bizonrelax.primewalker2.core;

import java.util.List;

/** Фабрика визуализаций (порт core.VisualizerFactory). */
public final class VisualizerFactory {

    private VisualizerFactory() {}

    public static List<BaseVisualizer> all() {
        return List.of(new UlamSpiralVisualizer(), new JacobsLadderVisualizer(), new PrimeWalkVisualizer());
    }

    /** Поиск по displayName без учёта регистра; неточное совпадение — Ulam Spiral (значение по умолчанию ТЗ). */
    public static BaseVisualizer byName(String name) {
        for (var v : all()) {
            if (v.displayName().equalsIgnoreCase(name)) return v;
        }
        return new UlamSpiralVisualizer();
    }
}
