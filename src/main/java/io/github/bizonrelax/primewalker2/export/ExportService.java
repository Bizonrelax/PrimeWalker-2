package io.github.bizonrelax.primewalker2.export;

import io.github.bizonrelax.primewalker2.core.VisualizationResult;
import io.github.bizonrelax.primewalker2.render.RenderOptions;
import io.github.bizonrelax.primewalker2.render.SheetRenderer;
import io.github.bizonrelax.primewalker2.render.TilingPlan;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Экспорт готовых изображений и печатных пакетов (фрагмент 004, ТЗ «Печать»):
 * PNG/JPG отдельным файлом, PDF с bleed 3 мм и crop marks (собственный writer,
 * см. PdfWriter / AUDIT.md решение 2), ZIP-архив листов тесселяции с
 * assembly_instructions.txt. Ошибки доступа к файлу возвращаются в
 * {@link ExportOutcome#error()} без исключения наружу (ТЗ).
 */
public final class ExportService {

    /** Вынос содержимого за линию реза (bleed), мм — ТЗ: 3 мм. */
    public static final double BLEED_MM = 3.0;

    /**
     * Экспортирует трассу как растровое изображение (PNG или JPG) в file.
     * Масштабирование по границам трассы: everything within bounds + поля 1 ед.
     *
     * @param result   результат визуализации (path в координатах сетки)
     * @param opts     параметры рендеринга (widthPx/heightPx/dpi/цвета)
     * @param format   "png" или "jpg"
     * @param file     целевой путь
     * @return итог; при ошибке доступа — ok()==false с сообщением в error()
     */
    public ExportOutcome exportImage(VisualizationResult result, RenderOptions opts,
                                     String format, Path file) {
        String fmt = normalizeFormat(format);
        if (!fmt.equals("png") && !fmt.equals("jpg")) {
            return ExportOutcome.fail(fmt, file, "Неподдерживаемый формат изображения: " + format);
        }
        try {
            BufferedImage img = renderPoster(result, opts);
            ImageIO.write(img, fmt, file.toFile());
            return ExportOutcome.done(fmt, file, Files.size(file), 1);
        } catch (FileSystemException e) {
            return ExportOutcome.fail(fmt, file, describe(e, file));
        } catch (IOException e) {
            return ExportOutcome.fail(fmt, file, describe(e, file));
        } catch (RuntimeException e) {
            return ExportOutcome.fail(fmt, file, "Ошибка рендеринга: " + e.getMessage());
        }
    }

    /**
     * Экспортирует постер в PDF: каждый лист A4 плана тесселяции — страница
     * 595.28x841.89 pt; содержимое постера растянуто с bleed {@value #BLEED_MM} мм
     * за линии реза; crop marks на каждом листе (SheetRenderer).
     */
    public ExportOutcome exportPdf(VisualizationResult result, RenderOptions opts, Path file) {
        return exportPdf(result, opts, file, null);
    }

    /** То же, с явным планом тесселяции (для больших постеров вне памяти холста). */
    public ExportOutcome exportPdf(VisualizationResult result, RenderOptions opts, Path file,
                                   TilingPlan planOverride) {
        try {
            TilingPlan plan = planOverride != null ? planOverride
                    : TilingPlan.forImage(opts.widthPx(), opts.heightPx(), opts.dpi());
            BufferedImage poster = renderPoster(result, plan.sheetWidthPx() * plan.sheetsWide(),
                    plan.sheetHeightPx() * plan.sheetsHigh(), opts);
            var writer = new PdfWriter();
            double wPt = TilingPlan.A4_WIDTH_MM / 25.4 * 72.0;
            double hPt = TilingPlan.A4_HEIGHT_MM / 25.4 * 72.0;
            int sheets = 0;
            for (int row = 0; row < plan.sheetsHigh(); row++) {
                for (int col = 0; col < plan.sheetsWide(); col++) {
                    writer.addPage(wPt, hPt, composeSheet(poster, plan, col, row, opts));
                    sheets++;
                }
            }
            try (var out = Files.newOutputStream(file)) {
                writer.write(out);
            }
            return ExportOutcome.done("pdf", file, Files.size(file), sheets);
        } catch (FileSystemException e) {
            return ExportOutcome.fail("pdf", file, describe(e, file));
        } catch (IOException e) {
            return ExportOutcome.fail("pdf", file, describe(e, file));
        } catch (RuntimeException e) {
            return ExportOutcome.fail("pdf", file, "Ошибка формирования PDF: " + e.getMessage());
        }
    }

    /**
     * Экспортирует ZIP-архив плиток для ручной сборки постера: листы
     * sheet_R{row+1}_C{col+1}.png (в порядке чтения слева направо, сверху вниз)
     * и assembly_instructions.txt с картой раскладки.
     */
    public ExportOutcome exportTilingZip(VisualizationResult result, RenderOptions opts, Path file) {
        return exportTilingZip(result, opts, file, null);
    }

    /** То же, с явным планом тесселяции. */
    public ExportOutcome exportTilingZip(VisualizationResult result, RenderOptions opts, Path file,
                                         TilingPlan planOverride) {
        try {
            TilingPlan plan = planOverride != null ? planOverride
                    : TilingPlan.forImage(opts.widthPx(), opts.heightPx(), opts.dpi());
            BufferedImage poster = renderPoster(result, plan.sheetWidthPx() * plan.sheetsWide(),
                    plan.sheetHeightPx() * plan.sheetsHigh(), opts);
            try (var zos = new ZipOutputStream(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                for (int row = 0; row < plan.sheetsHigh(); row++) {
                    for (int col = 0; col < plan.sheetsWide(); col++) {
                        var bos = new ByteArrayOutputStream();
                        ImageIO.write(composeSheet(poster, plan, col, row, opts), "png", bos);
                        zos.putNextEntry(new ZipEntry(
                                String.format("sheet_R%d_C%d.png", row + 1, col + 1)));
                        zos.write(bos.toByteArray());
                        zos.closeEntry();
                    }
                }
                zos.putNextEntry(new ZipEntry("assembly_instructions.txt"));
                zos.write(instructions(plan).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            return ExportOutcome.done("zip", file, Files.size(file), plan.totalSheets());
        } catch (FileSystemException e) {
            return ExportOutcome.fail("zip", file, describe(e, file));
        } catch (IOException e) {
            return ExportOutcome.fail("zip", file, describe(e, file));
        } catch (RuntimeException e) {
            return ExportOutcome.fail("zip", file, "Ошибка архивации: " + e.getMessage());
        }
    }

    // ---------- внутреннее ----------

    private static String normalizeFormat(String format) {
        if (format == null) return "";
        String f = format.trim().toLowerCase(java.util.Locale.US);
        if (f.equals("jpeg")) f = "jpg";
        return f;
    }

    private static String describe(IOException e, Path file) {
        String m = e.getMessage();
        return (m == null || m.isBlank() ? e.getClass().getSimpleName() : m)
                + " [" + file + "]";
    }

    /**
     * Растеризует трассу в постер widthPx x heightPx: аффинное соответствие
     * границ трассы (+поле 1 ед. сетки) на холст, сохраняющее пропорции
     * (центрирование по меньшей измеренной оси).
     */
    static BufferedImage renderPoster(VisualizationResult result, RenderOptions opts) {
        return renderPoster(result, opts.widthPx(), opts.heightPx(), opts);
    }

    static BufferedImage renderPoster(VisualizationResult result, int canvasW, int canvasH,
                                      RenderOptions opts) {
        var b = result.bounds();
        double pad = 1.0;
        double minX = b.minX() - pad, minY = b.minY() - pad;
        double bw = Math.max(b.width() + 2 * pad, 1e-9);
        double bh = Math.max(b.height() + 2 * pad, 1e-9);
        double sx = canvasW / bw;
        double sy = canvasH / bh;
        double s = Math.min(sx, sy);
        double tx = (canvasW - bw * s) / 2.0 - minX * s;
        double ty = (canvasH - bh * s) / 2.0 - minY * s;
        AffineTransform at = new AffineTransform(s, 0, 0, s, tx, ty);
        List<Point2D.Double> pts = result.path();
        Point2D.Double[] mapped = new Point2D.Double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            Point2D m = at.transform(pts.get(i), new Point2D.Double());
            mapped[i] = (Point2D.Double) m;
        }
        var canvasOpts = new RenderOptions(canvasW, canvasH, opts.dpi(), opts.background(),
                opts.primeColor(), opts.endColor(), opts.pathColor(), opts.modulo(),
                opts.pointSizePx(), opts.drawPath());
        var svc = new io.github.bizonrelax.primewalker2.render.RenderService();
        return svc.renderImage(List.of(mapped), result.primeFlags(), result.values(),
                new io.github.bizonrelax.primewalker2.render.Pose(1, 1, 0, 0), canvasOpts);
    }

    /**
     * Собирает лист (col,row) из постера: холст A4, содержимое области листа со
     * смещением bleed (картинка выходит за линии реза на BLEED_MM — ТЗ), поверх —
     * crop/registration метки SheetRenderer.
     */
    private static BufferedImage composeSheet(BufferedImage poster, TilingPlan plan,
                                              int col, int row, RenderOptions opts) {
        double pxPerMm = plan.dpi() / 25.4;
        // левый верхний угол области постера для листа (мм от края листа);
        // минус bleed => картинка заходит за линию реза на BLEED_MM
        double srcXmm = col * plan.stepXmm() - TilingPlan.PRINTER_UNPRINTABLE_MM - BLEED_MM;
        double srcYmm = row * plan.stepYmm() - TilingPlan.PRINTER_UNPRINTABLE_MM - BLEED_MM;
        int sw = plan.sheetWidthPx(), sh = plan.sheetHeightPx();
        var sheet = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setColor(opts.background());
            g.fillRect(0, 0, sw, sh);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(poster, (int) Math.round(-srcXmm * pxPerMm),
                    (int) Math.round(-srcYmm * pxPerMm), null);
        } finally {
            g.dispose();
        }
        return new SheetRenderer().renderMarks(sheet, plan, col, row, opts);
    }

    private static String instructions(TilingPlan plan) {
        var sb = new StringBuilder();
        sb.append("PrimeWalker-2 - сборка постера из листов A4\n");
        sb.append("==============================================\n\n");
        sb.append("Листов: " + plan.totalSheets() + " (" + plan.sheetsWide()
                + " колонок x " + plan.sheetsHigh() + " строк), разрешение "
                + plan.dpi() + " DPI\n");
        sb.append("Перекрытие соседних листов: " + (int) TilingPlan.OVERLAP_MM
                + " мм; неизвлекаемое поле принтера: "
                + (int) TilingPlan.PRINTER_UNPRINTABLE_MM + " мм.\n\n");
        sb.append("Порядок склейки (листы нумеруются R=строка, C=колонка, с 1):\n");
        for (int row = 0; row < plan.sheetsHigh(); row++) {
            for (int col = 0; col < plan.sheetsWide(); col++) {
                sb.append("  [R").append(row + 1).append(",C").append(col + 1).append(']');
            }
            sb.append('\n');
        }
        sb.append("\nИнструкция:\n");
        sb.append("1. Проверьте масштаб печати 100% (без вписывания на страницу).\n");
        sb.append("2. Совместите обрезные метки соседних листов по перекрытию.\n");
        sb.append("3. Проклейте стыки с обратной стороны; начинайте с верхнего левого угла.\n");
        return sb.toString();
    }
}
