package io.github.bizonrelax.primewalker2.persist;

import java.time.Duration;
import java.time.Instant;

/**
 * Глобальная статистика использования поверх AppSettings (ключи stats/*, см. ТЗ и PORTS.md).
 * Анонимна: никаких персональных данных. Хранится вместе с настройками.
 */
public final class GlobalStats {

    private static final String FIRST_RUN = "stats/firstRun";
    private static final String RUNTIME = "stats/totalRuntimeSec";
    private static final String LAST_TS = "stats/lastSessionTs";
    private static final String LARGE_W = "stats/largestImageWidth";
    private static final String LARGE_H = "stats/largestImageHeight";
    private static final String STEPS = "stats/totalStepsDrawn";
    private static final String ALG_PREFIX = "stats/algoCount.";
    private static final String FMT_PREFIX = "stats/exportFormatCount.";

    private final AppSettings st;

    public GlobalStats(AppSettings st) { this.st = st; }

    /** Зафиксировать первый запуск (метрика не меняется никогда) и открыть новую сессию. */
    public void sessionStarted() {
        if (st.getString(FIRST_RUN, "").isEmpty()) st.put(FIRST_RUN, Instant.now().toString());
        st.put(LAST_TS, Instant.now().toString());
    }

    /** Закрыть сессию: прибавить пережитое время к общей длительности работы. */
    public void sessionEnded() {
        String last = st.getString(LAST_TS, "");
        if (!last.isEmpty()) {
            try {
                long sec = Duration.between(Instant.parse(last), Instant.now()).getSeconds();
                if (sec > 0) st.put(RUNTIME, st.getLong(RUNTIME, 0) + sec);
            } catch (RuntimeException e) { /* битая метка — пропускаем сессию */ }
        }
        st.put(LAST_TS, Instant.now().toString());
    }

    public Instant firstRun() {
        String s = st.getString(FIRST_RUN, "");
        try { return s.isEmpty() ? null : Instant.parse(s); } catch (RuntimeException e) { return null; }
    }

    public long totalRuntimeSec() { return st.getLong(RUNTIME, 0); }

    public long totalStepsDrawn() { return st.getLong(STEPS, 0); }

    public void addStepsDrawn(long n) { if (n > 0) st.put(STEPS, st.getLong(STEPS, 0) + n); }

    /** Обновить рекорд максимального размера изображения (сравнение по площади). */
    public void imageGenerated(int w, int h) {
        long area = (long) w * h;
        if (area > st.getLong(LARGE_AREA, 0)) { st.put(LARGE_AREA, area); st.put(LARGE_W, w); st.put(LARGE_H, h); }
    }

    private static final String LARGE_AREA = "stats/largestImageArea";

    public int largestImageWidth()  { return st.getInt(LARGE_W, 0, 0, Integer.MAX_VALUE); }
    public int largestImageHeight() { return st.getInt(LARGE_H, 0, 0, Integer.MAX_VALUE); }

    public void algorithmUsed(String name) {
        String k = ALG_PREFIX + safe(name);
        st.put(k, st.getLong(k, 0) + 1);
    }

    public long algorithmCount(String name) { return st.getLong(ALG_PREFIX + safe(name), 0); }

    public void formatUsed(String fmt) {
        String k = FMT_PREFIX + safe(fmt);
        st.put(k, st.getLong(k, 0) + 1);
    }

    public long formatCount(String fmt) { return st.getLong(FMT_PREFIX + safe(fmt), 0); }

    /** Кнопка «Сбросить статистику» из ТЗ: чистит только stats/*. */
    public void reset() {
        for (String k : st.stringPropertyNames()) {
            if (k.startsWith("stats/")) st.remove(k);
        }
    }

    private static String safe(String s) { return s.replaceAll("[^A-Za-z0-9_.-]", "_"); }
}
