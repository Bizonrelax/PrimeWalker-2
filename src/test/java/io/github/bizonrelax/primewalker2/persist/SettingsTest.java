package io.github.bizonrelax.primewalker2.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты AppSettings: round-trip, валидация диапазонов, история экспорта. */
class SettingsTest {

    @TempDir Path tmp;

    @Test void roundTripKeepsValues() {
        Path f = tmp.resolve("settings.properties");
        AppSettings a = new AppSettings(f);
        a.put("ui/startNumber", 7);
        a.put("ui/scale", 2.5);
        a.put("ui/theme", "dark");
        a.save();

        AppSettings b = new AppSettings(f);
        b.load();
        assertEquals(7, b.startNumber());
        assertEquals(2.5, b.scale());
        assertEquals("dark", b.theme());
    }

    @Test void invalidValuesFallBackToDefaults() {
        AppSettings a = new AppSettings(tmp.resolve("s2.properties"));
        a.put("ui/fontSize", -5);          // вне диапазона 8..16
        a.put("ui/scale", 99.0);           // вне диапазона 0.1..10
        a.put("ui/stepCount", 0);          // минимум 1
        a.put("ui/startNumber", "abc");    // не число
        assertEquals(12, a.fontSize());
        assertEquals(1.0, a.scale());
        assertEquals(1000, a.stepCount());
        assertEquals(1, a.startNumber());
    }

    @Test void missingFileGivesDefaults() {
        AppSettings a = new AppSettings(tmp.resolve("nope.properties"));
        a.load();
        assertEquals("Ulam Spiral", a.visualizerType());
        assertEquals("#FF0000", a.primeColor());
        assertEquals(6, a.colorModulo());
        assertEquals(-1, a.windowX());
    }

    @Test void exportHistoryRoundTripAndCap() {
        Path f = tmp.resolve("s3.properties");
        AppSettings a = new AppSettings(f);
        for (int i = 0; i < 60; i++) {
            a.addExport(new AppSettings.ExportRecord(Instant.now().toString(), "/p/img" + i + ".png", 100 + i, 200, 1.5));
        }
        var h = a.exportHistory();
        assertEquals(50, h.size(), "история ограничена 50 записями");
        assertEquals("/p/img59.png", h.peekLast().filePath());
        a.save();

        AppSettings b = new AppSettings(f);
        b.load();
        var hb = b.exportHistory();
        assertEquals(50, hb.size());
        var last = hb.peekLast();
        assertEquals(159, last.imageWidth());
        assertEquals(1.5, last.fileSizeMB());
    }

    @Test void corruptHistoryLineSkipped() {
        AppSettings a = new AppSettings(tmp.resolve("s4.properties"));
        a.put("export/history", "2026-01-01T00:00:00Z,/x.png,10,20,0.5;junk-line;2026-01-02T00:00:00Z,/y.png,30,40,1.0");
        assertEquals(2, a.exportHistory().size());
    }
}
