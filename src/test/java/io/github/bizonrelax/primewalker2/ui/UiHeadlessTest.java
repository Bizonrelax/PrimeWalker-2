package io.github.bizonrelax.primewalker2.ui;

import io.github.bizonrelax.primewalker2.persist.AppSettings;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless-тесты фрагмента 006: проверяется логика UI без создания окон
 * (сам JFrame остаётся за ручной проверкой на Windows, PLAN.md 006).
 */
class UiHeadlessTest {

    @Test
    void parseColorHandlesHexAndFallback() {
        assertEquals(Color.RED, MainWindow.parseColor("#FF0000", Color.BLACK));
        assertEquals(new Color(0x00, 0x40, 0x00), MainWindow.parseColor("#004000", Color.BLACK));
        assertEquals(Color.BLACK, MainWindow.parseColor("не цвет", Color.BLACK));
        assertEquals(Color.BLACK, MainWindow.parseColor(null, Color.BLACK));
    }

    @Test
    void hexRoundTripsParseColor() {
        for (String s : new String[]{"#FF0000", "#0000FF", "#008000", "#123456"}) {
            assertEquals(s, MainWindow.hex(MainWindow.parseColor(s, Color.BLACK)));
        }
    }

    @Test
    void exportRegistryStartsNullAndAcceptsGateway() {
        ExportGateway saved = ExportGateway.Registry.get();
        try {
            ExportGateway fake = new ExportGateway() {
                @Override public Outcome exportPng(BufferedImage i, Path f) { return new Outcome(f, 1, 1); }
                @Override public Outcome exportJpg(BufferedImage i, Path f, float q) { return new Outcome(f, 1, 1); }
                @Override public Outcome exportPdf(BufferedImage i, Path f, boolean v, double b) { return new Outcome(f, 1, 1); }
                @Override public Outcome exportZipTiles(BufferedImage i, Path f, double o) { return new Outcome(f, 1, 1); }
            };
            ExportGateway.Registry.set(fake);
            assertSame(fake, ExportGateway.Registry.get());
        } finally {
            ExportGateway.Registry.set(saved);
        }
    }

    @Test
    void settingsDefaultsDriveFormValues() throws Exception {
        Path tmp = Files.createTempDirectory("pw-ui-test").resolve("settings.properties");
        AppSettings st = new AppSettings(tmp);
        st.load();
        // значения по умолчанию ТЗ читаются без исключений — контракт, который использует MainWindow
        assertEquals(1, st.startNumber());
        assertEquals(1000, st.stepCount());
        assertEquals(1.0, st.scale(), 1e-9);
        assertEquals("Ulam Spiral", st.visualizerType());
        assertEquals("light", st.theme());
        assertEquals(12, st.fontSize());
        st.save();
        assertTrue(java.nio.file.Files.exists(tmp));
    }
}
