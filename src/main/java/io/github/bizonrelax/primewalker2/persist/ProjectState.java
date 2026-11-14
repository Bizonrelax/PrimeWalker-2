package io.github.bizonrelax.primewalker2.persist;

import java.time.Instant;

/** Полное состояние проекта для файла .pwalk (владелец формата — фрагмент 005, см. GVK/PORTS.md). */
public record ProjectState(
    int formatVersion,
    long initialNumber,
    int stepCount,
    double scale,
    String visualizerType,
    Colors colors,
    int colorModulo,
    Instant savedAt,
    String imagePath) {

    public static final int CURRENT_VERSION = 1;

    public static ProjectState of(long n, int steps, double scale, String viz, Colors c, int modulo, String img) {
        return new ProjectState(CURRENT_VERSION, n, steps, scale, viz, c, modulo, Instant.now(), img);
    }
}
