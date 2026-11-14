package io.github.bizonrelax.primewalker2.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты GlobalStats: firstRun неизменяемость, счётчики алгоритмов/форматов, сброс. */
class StatsTest {

    private AppSettings fresh() { return new AppSettings(Path.of(System.getProperty("java.io.tmpdir"), "pw-stats-" + System.nanoTime() + ".properties")); }

    @Test void firstRunFixedOnce() {
        AppSettings a = fresh();
        GlobalStats g = new GlobalStats(a);
        assertNull(g.firstRun());
        g.sessionStarted();
        assertNotNull(g.firstRun());
        var first = g.firstRun();
        g.sessionStarted();
        assertEquals(first, g.firstRun(), "firstRun не меняется при повторных запусках");
    }

    @Test void countersAccumulate() {
        GlobalStats g = new GlobalStats(fresh());
        g.algorithmUsed("Ulam Spiral");
        g.algorithmUsed("Ulam Spiral");
        g.algorithmUsed("Jacob's Ladder");
        g.formatUsed("PDF");
        g.addStepsDrawn(1000);
        g.addStepsDrawn(500);
        assertEquals(2, g.algorithmCount("Ulam Spiral"));
        assertEquals(1, g.algorithmCount("Jacob's Ladder"));
        assertEquals(1, g.formatCount("PDF"));
        assertEquals(1500, g.totalStepsDrawn());
    }

    @Test void largestImageRecordOnlyGrows() {
        GlobalStats g = new GlobalStats(fresh());
        g.imageGenerated(1000, 1000);
        g.imageGenerated(800, 800);    // меньше — игнор
        g.imageGenerated(200, 6000);   // больше по площади (1.2M > 1.0M)
        assertEquals(200, g.largestImageWidth());
        assertEquals(6000, g.largestImageHeight());
    }

    @Test void resetClearsStatsOnly() {
        AppSettings a = fresh();
        a.put("ui/theme", "dark");
        GlobalStats g = new GlobalStats(a);
        g.sessionStarted();
        g.addStepsDrawn(10);
        g.reset();
        assertEquals(0, g.totalStepsDrawn());
        assertNull(g.firstRun());
        assertEquals("dark", a.theme(), "сброс статистики не трогает настройки UI");
    }

    @Test void runtimeSurvivesRoundTrip() {
        Path f = Path.of(System.getProperty("java.io.tmpdir"), "pw-stats-" + System.nanoTime() + ".properties");
        AppSettings a = new AppSettings(f);
        GlobalStats g = new GlobalStats(a);
        g.sessionStarted();
        a.put("stats/totalRuntimeSec", 120);
        a.save();
        AppSettings b = new AppSettings(f);
        b.load();
        assertEquals(120, new GlobalStats(b).totalRuntimeSec());
    }
}
