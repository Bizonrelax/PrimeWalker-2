package io.github.bizonrelax.primewalker2.persist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Сохранение/загрузка проекта .pwalk (JSON UTF-8, формат — GVK/PORTS.md).
 * Собственный минимальный JSON-писатель/читатель: внешних библиотек нет (переносимость).
 */
public final class ProjectManager {

    public void saveProject(Path pwalk, ProjectState s) {
        StringBuilder j = new StringBuilder(256);
        j.append("{\n");
        num(j, "formatVersion", String.valueOf(s.formatVersion()), true);
        num(j, "initial_number", String.valueOf(s.initialNumber()), true);
        num(j, "step_count", String.valueOf(s.stepCount()), true);
        num(j, "scale", fmtD(s.scale()), true);
        str(j, "visualizer_type", s.visualizerType(), true);
        j.append("  \"colors\": {\"start\": ").append(q(s.colors().start()))
         .append(", \"end\": ").append(q(s.colors().end()))
         .append(", \"path\": ").append(q(s.colors().path())).append("},\n");
        num(j, "color_modulo", String.valueOf(s.colorModulo()), true);
        str(j, "saved_at", s.savedAt() == null ? Instant.now().toString() : s.savedAt().toString(), true);
        str(j, "image_path", s.imagePath() == null ? "" : s.imagePath(), false);
        j.append("}\n");
        try {
            Path dir = pwalk.toAbsolutePath().getParent();
            if (dir != null) Files.createDirectories(dir);
            Files.writeString(pwalk, j.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new UncheckedIOException("не удалось записать проект: " + pwalk, e); }
    }

    public ProjectState loadProject(Path pwalk) {
        String src;
        try { src = Files.readString(pwalk, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new UncheckedIOException("не удалось прочитать проект: " + pwalk, e); }
        JsonReader r = new JsonReader(src);
        var o = r.object();
        int ver = (int) o.num("formatVersion", ProjectState.CURRENT_VERSION);
        long n = (long) o.num("initial_number", 1);
        int steps = (int) o.num("step_count", 1000);
        double scale = o.num("scale", 1.0);
        String viz = o.str("visualizer_type", "Ulam Spiral");
        var c = o.obj("colors");
        Colors colors = new Colors(c.str("start", "#FF0000"), c.str("end", "#0000FF"), c.str("path", "#000000"));
        int modulo = (int) o.num("color_modulo", 6);
        Instant savedAt;
        try { savedAt = Instant.parse(o.str("saved_at", "")); } catch (RuntimeException e) { savedAt = Instant.EPOCH; }
        String img = o.str("image_path", "");
        return new ProjectState(ver, n, steps, scale, viz, colors, modulo, savedAt, img);
    }

    private static void num(StringBuilder j, String k, String v, boolean comma) {
        j.append("  \"").append(k).append("\": ").append(v).append(comma ? ",\n" : "\n");
    }

    /** double без хвоста: 1.0 -> "1.0", 3.5 -> "3.5". */
    private static String fmtD(double d) { return (d == Math.rint(d)) ? String.valueOf(d) : String.valueOf(d); }

    private static void str(StringBuilder j, String k, String v, boolean comma) {
        j.append("  \"").append(k).append("\": ").append(q(v)).append(comma ? ",\n" : "\n");
    }

    private static String q(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    /** Минимальный reader плоского JSON-объекта с одним уровнем вложенности (достаточно формата .pwalk). */
    static final class JsonReader {
        private final String s;
        private int i;
        JsonReader(String s) { this.s = s; }

        Obj object() { ws(); expect('{'); Obj o = new Obj(); parseFields(o); return o; }

        private void parseFields(Obj o) {
            ws();
            if (peek() == '}') { i++; return; }
            while (true) {
                ws();
                String k = string();
                ws(); expect(':');
                ws();
                char c = peek();
                if (c == '{') { i++; Obj inner = new Obj(); parseFields(inner); o.put(k, inner); }
                else if (c == '"') { o.put(k, string()); }
                else { o.put(k, number()); }
                ws();
                if (peek() == ',') { i++; continue; }
                if (peek() == '}') { i++; return; }
                throw err("ожидался , или }");
            }
        }

        private String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> b.append('\n');
                        case 'r' -> b.append('\r');
                        case 't' -> b.append('\t');
                        case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> b.append(e);
                    }
                } else b.append(c);
            }
        }

        private double number() {
            int st = i;
            while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(st, i));
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char peek() { if (i >= s.length()) throw err("конец файла"); return s.charAt(i); }
        private void expect(char c) { ws(); if (peek() != c) throw err("ожидался '" + c + "'"); i++; }
        private RuntimeException err(String m) { return new IllegalArgumentException("битый JSON в .pwalk (" + m + ") на позиции " + i); }

        static final class Obj {
            private final java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            void put(String k, Object v) { m.put(k, v); }
            Obj obj(String k) { Object v = m.get(k); return v instanceof Obj o ? o : new Obj(); }
            String str(String k, String def) { Object v = m.get(k); return v instanceof String s ? s : def; }
            double num(String k, double def) { Object v = m.get(k); return v instanceof Number n ? n.doubleValue() : def; }
        }
    }
}
