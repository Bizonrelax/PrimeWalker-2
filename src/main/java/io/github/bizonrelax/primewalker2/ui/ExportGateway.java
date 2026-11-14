package io.github.bizonrelax.primewalker2.ui;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Мост фрагмента 006 к сервису экспорта фрагмента 004 (порт export.ExportService).
 * Реализация подставляется извне; если модуль 004 ещё не слит — UI сообщает об отсутствии экспорта.
 */
public interface ExportGateway {

    record Outcome(Path file, long bytes, int sheets) {}

    Outcome exportPng(BufferedImage img, Path file);

    Outcome exportJpg(BufferedImage img, Path file, float quality);

    /** PDF/X-совместимый вывод: bleed 3 мм, crop marks, векторный/растровый режим по размеру. */
    Outcome exportPdf(BufferedImage img, Path file, boolean vector, double bleedMm);

    /** ZIP плиток A4 с монтажными метками и assembly_instructions. */
    Outcome exportZipTiles(BufferedImage img, Path zipFile, double overlapMm);

    /** Реестр доступных реализаций: Main регистрирует мост к пакету export после слияния 004. */
    final class Registry {
        private static volatile ExportGateway current;

        private Registry() {}

        public static void set(ExportGateway g) { current = g; }

        public static ExportGateway get() { return current; }
    }
}
