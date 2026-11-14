package io.github.bizonrelax.primewalker2.export;

import java.nio.file.Path;

/**
 * Итог экспорта (порт export.ExportOutcome): формат, файл, размер в байтах,
 * число листов постера и сообщение об ошибке доступа (ТЗ: запись файла с
 * ошибками доступа — без исключения наружу).
 */
public record ExportOutcome(String format, Path file, long bytes, int sheets, String error) {

    public boolean ok() {
        return error == null;
    }

    static ExportOutcome fail(String format, Path file, String msg) {
        return new ExportOutcome(format, file, 0, 0, msg);
    }

    static ExportOutcome done(String format, Path file, long bytes, int sheets) {
        return new ExportOutcome(format, file, bytes, sheets, null);
    }
}
