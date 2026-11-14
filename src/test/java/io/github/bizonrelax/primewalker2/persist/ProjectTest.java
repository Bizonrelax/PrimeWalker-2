package io.github.bizonrelax.primewalker2.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Юнит-тесты ProjectManager: формат .pwalk (JSON), round-trip, обратная совместимость версии. */
class ProjectTest {

    @TempDir Path tmp;

    @Test void saveLoadRoundTrip() throws Exception {
        ProjectState s = ProjectState.of(101L, 50_000, 3.5, "Jacob's Ladder",
            new Colors("#112233", "#445566", "#778899"), 7, "/pics/out.png");
        Path p = tmp.resolve("project.pwalk");
        ProjectManager pm = new ProjectManager();
        pm.saveProject(p, s);

        String raw = Files.readString(p, StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"formatVersion\": 1"), raw);
        assertTrue(raw.contains("Jacob's Ladder"));

        ProjectState l = pm.loadProject(p);
        assertEquals(s.formatVersion(), l.formatVersion());
        assertEquals(101L, l.initialNumber());
        assertEquals(50_000, l.stepCount());
        assertEquals(3.5, l.scale());
        assertEquals("Jacob's Ladder", l.visualizerType());
        assertEquals(new Colors("#112233", "#445566", "#778899"), l.colors());
        assertEquals(7, l.colorModulo());
        assertEquals("/pics/out.png", l.imagePath());
        assertNotNull(l.savedAt());
    }

    @Test void loadMissingFieldsUsesDefaults() throws Exception {
        Path p = tmp.resolve("minimal.pwalk");
        Files.writeString(p, "{\"formatVersion\": 1, \"initial_number\": 42}", StandardCharsets.UTF_8);
        ProjectState l = new ProjectManager().loadProject(p);
        assertEquals(42L, l.initialNumber());
        assertEquals(1000, l.stepCount());
        assertEquals("Ulam Spiral", l.visualizerType());
        assertEquals(Colors.defaults(), l.colors());
    }

    @Test void escapesSpecialCharsInStrings() throws Exception {
        Path p = tmp.resolve("esc.pwalk");
        ProjectState s = ProjectState.of(2L, 10, 1.0, "Weird \"name\"\\path", Colors.defaults(), 6, "C:\\a\nb.png");
        new ProjectManager().saveProject(p, s);
        ProjectState l = new ProjectManager().loadProject(p);
        assertEquals("Weird \"name\"\\path", l.visualizerType());
        assertEquals("C:\\a\nb.png", l.imagePath());
    }

    @Test void brokenJsonThrowsClearError() throws Exception {
        Path p = tmp.resolve("bad.pwalk");
        Files.writeString(p, "{ not json ", StandardCharsets.UTF_8);
        var e = assertThrows(RuntimeException.class, () -> new ProjectManager().loadProject(p));
        assertTrue(e.getMessage().contains(".pwalk") || (e.getCause() != null && e.getCause().getMessage().contains(".pwalk")));
    }

    @Test void savesIntoMissingDirectory() {
        Path p = tmp.resolve("deep/nested/dir/project.pwalk");
        new ProjectManager().saveProject(p, ProjectState.of(1L, 1, 1.0, "Prime Walk", Colors.defaults(), 6, ""));
        assertTrue(Files.exists(p));
    }
}
