package io.github.bizonrelax.primewalker2.persist;

/** Набор сохраняемых цветов визуализации (start — начальная точка, end — конечная, path — трасса). */
public record Colors(String start, String end, String path) {
    public static Colors defaults() { return new Colors("#FF0000", "#0000FF", "#000000"); }
}
