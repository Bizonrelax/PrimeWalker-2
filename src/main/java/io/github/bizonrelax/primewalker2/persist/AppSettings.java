package io.github.bizonrelax.primewalker2.persist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Настройки приложения: аналог QSettings на java.util.Properties, файл в per-user каталоге.
 * Валидация значений при чтении: некорректное значение заменяется default (защита от краш-цикла).
 */
public final class AppSettings {

    public static final String ORG = "Bizonrelax";
    public static final String APP = "PrimeWalker-2";

    private final Path file;
    private final Properties p = new Properties();

    public AppSettings() { this(defaultFile()); }

    public AppSettings(Path file) { this.file = file; }

    /** `~/.config/Bizonrelax/PrimeWalker-2/settings.properties` (или $PRIMEWALKER2_HOME). */
    public static Path defaultFile() {
        Path dir = System.getenv("PRIMEWALKER2_HOME") != null
            ? Path.of(System.getenv("PRIMEWALKER2_HOME"))
            : Path.of(System.getProperty("user.home"), ".config", ORG, APP);
        return dir.resolve("settings.properties");
    }

    public void load() {
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            catch (IOException e) { /* пустой набор — все значения по умолчанию */ }
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "PrimeWalker-2 settings");
            }
        } catch (IOException e) { throw new SettingsException("не удалось сохранить настройки: " + file, e); }
    }

    public Path file() { return file; }

    // ---------- чтение с валидацией ----------

    public int getInt(String key, int def, int min, int max) {
        String s = p.getProperty(key);
        if (s == null) return def;
        try {
            int v = Integer.parseInt(s.trim());
            return (v < min || v > max) ? def : v;
        } catch (NumberFormatException e) { return def; }
    }

    public long getLong(String key, long def) {
        String s = p.getProperty(key);
        if (s == null) return def;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public double getDouble(String key, double def, double min, double max) {
        String s = p.getProperty(key);
        if (s == null) return def;
        try {
            double v = Double.parseDouble(s.trim());
            return (v < min || v > max) ? def : v;
        } catch (NumberFormatException e) { return def; }
    }

    public String getString(String key, String def) {
        String s = p.getProperty(key);
        return (s == null || s.isEmpty()) ? def : s;
    }

    public boolean getBool(String key, boolean def) {
        String s = p.getProperty(key);
        if (s == null) return def;
        s = s.trim();
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        return def;
    }

    public void put(String key, Object value) { p.setProperty(key, String.valueOf(value)); }

    public void remove(String key) { p.remove(key); }

    public java.util.Set<String> stringPropertyNames() { return p.stringPropertyNames(); }

    // ---------- типизированные геттеры по ключам ТЗ ----------

    public int startNumber()      { return getInt("ui/startNumber", 1, Integer.MIN_VALUE, Integer.MAX_VALUE); }
    public int stepCount()        { return getInt("ui/stepCount", 1000, 1, 100_000_000); }
    public double scale()         { return getDouble("ui/scale", 1.0, 0.1, 10.0); }
    public String visualizerType(){ return getString("ui/visualizerType", "Ulam Spiral"); }
    public String theme()         { return getString("ui/theme", "light"); }
    public int fontSize()         { return getInt("ui/fontSize", 12, 8, 16); }
    public String backgroundColor(){ return getString("ui/backgroundColor", "#FFFFFF"); }
    public String primeColor()    { return getString("ui/primeColor", "#FF0000"); }
    public String endColor()      { return getString("ui/endColor", "#0000FF"); }
    public String pathColor()     { return getString("ui/pathColor", "#000000"); }
    public int colorModulo()      { return getInt("ui/colorModulo", 6, 3, 8); }

    public int windowX()          { return getInt("window/x", -1, -1, Integer.MAX_VALUE); }
    public int windowY()          { return getInt("window/y", -1, -1, Integer.MAX_VALUE); }
    public int windowWidth()      { return getInt("window/width", 1024, 200, 100000); }
    public int windowHeight()     { return getInt("window/height", 768, 200, 100000); }

    // ---------- история экспорта (список записей ExportRecord, newest last) ----------

    public Deque<ExportRecord> exportHistory() {
        Deque<ExportRecord> d = new ArrayDeque<>();
        String raw = p.getProperty("export/history");
        if (raw != null && !raw.isBlank()) {
            for (String rec : raw.split("\\s*;\\s*")) {
                ExportRecord er = ExportRecord.parse(rec);
                if (er != null) d.addLast(er);
            }
        }
        return d;
    }

    public void addExport(ExportRecord r) {
        Deque<ExportRecord> d = exportHistory();
        d.addLast(r);
        while (d.size() > 50) d.pollFirst();
        StringBuilder sb = new StringBuilder();
        for (ExportRecord er : d) { if (sb.length() > 0) sb.append(';'); sb.append(er); }
        p.setProperty("export/history", sb.toString());
    }

    /** Запись истории экспорта: timestamp ISO8601, filePath, imageWidth, imageHeight, fileSizeMB. */
    public record ExportRecord(String timestamp, String filePath, int imageWidth, int imageHeight, double fileSizeMB) {
        @Override public String toString() {
            return timestamp + "," + filePath + "," + imageWidth + "," + imageHeight + "," + fileSizeMB;
        }
        static ExportRecord parse(String s) {
            String[] a = s.split(",", 5);
            if (a.length != 5) return null;
            try {
                return new ExportRecord(a[0].trim(), a[1].trim(),
                    Integer.parseInt(a[2].trim()), Integer.parseInt(a[3].trim()),
                    Double.parseDouble(a[4].trim()));
            } catch (RuntimeException e) { return null; }
        }
    }

    /** Класс исключения — чтобы IO-проблемы не сыпались наружу RuntimeException'ом JDK. */
    public static final class SettingsException extends RuntimeException {
        public SettingsException(String m, Throwable c) { super(m, c); }
    }
}
