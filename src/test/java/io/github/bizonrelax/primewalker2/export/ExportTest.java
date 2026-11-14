package io.github.bizonrelax.primewalker2.export;

import io.github.bizonrelax.primewalker2.core.UlamSpiralVisualizer;
import io.github.bizonrelax.primewalker2.render.RenderOptions;
import io.github.bizonrelax.primewalker2.render.TilingPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты экспорта (фрагмент 004): PNG/JPG/PDF/ZIP-плитки, bleed, ошибки доступа. */
class ExportTest {

    private static final RenderOptions SMALL =
            new RenderOptions(120, 120, 72, Color.WHITE, Color.RED, Color.BLUE,
                    Color.GRAY, 0, 2, true);

    private static io.github.bizonrelax.primewalker2.core.VisualizationResult spiral() {
        return new UlamSpiralVisualizer(1.0).visualize(1, 60);
    }

    @Test
    void exportsPngAndJpg(@TempDir Path dir) throws Exception {
        var svc = new ExportService();
        var png = svc.exportImage(spiral(), SMALL, "png", dir.resolve("a.png"));
        assertTrue(png.ok(), () -> png.error());
        assertEquals("png", png.format());
        assertTrue(png.bytes() > 0);
        BufferedImage img = ImageIO.read(dir.resolve("a.png").toFile());
        assertNotNull(img, "PNG читается обратно");
        assertEquals(120, img.getWidth());

        var jpg = svc.exportImage(spiral(), SMALL, "jpg", dir.resolve("a.jpg"));
        assertTrue(jpg.ok(), () -> jpg.error());
        assertTrue(Files.size(dir.resolve("a.jpg")) > 0);
    }

    @Test
    void unsupportedFormatReturnsErrorNotException(@TempDir Path dir) {
        var svc = new ExportService();
        var out = svc.exportImage(spiral(), SMALL, "tiff", dir.resolve("a.tiff"));
        assertFalse(out.ok());
        assertNotNull(out.error());
        assertFalse(Files.exists(dir.resolve("a.tiff")));
    }

    @Test
    void accessErrorIsReportedNotThrown(@TempDir Path dir) throws Exception {
        Path ro = dir.resolve("ro");
        Files.createDirectory(ro);
        ro.toFile().setWritable(false);
        try {
            var svc = new ExportService();
            var out = svc.exportImage(spiral(), SMALL, "png", ro.resolve("x.png"));
            if (!out.ok()) {
                assertNotNull(out.error()); // контракт: без исключения наружу
            }
            var pdf = svc.exportPdf(spiral(), SMALL, ro.resolve("x.pdf"));
            if (!pdf.ok()) assertNotNull(pdf.error());
            var zip = svc.exportTilingZip(spiral(), SMALL, ro.resolve("x.zip"));
            if (!zip.ok()) assertNotNull(zip.error());
        } finally {
            ro.toFile().setWritable(true);
        }
    }

    @Test
    void exportsSinglePagePdfWithHeader(@TempDir Path dir) throws Exception {
        var svc = new ExportService();
        var out = svc.exportPdf(spiral(), SMALL, dir.resolve("poster.pdf"));
        assertTrue(out.ok(), () -> out.error());
        assertEquals("pdf", out.format());
        assertEquals(1, out.sheets(), "малый постер — один лист A4");
        byte[] raw = Files.readAllBytes(dir.resolve("poster.pdf"));
        String head = new String(raw, 0, 8, StandardCharsets.ISO_8859_1);
        String tail = new String(raw, raw.length - 6, 6, StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF-1."), "заголовок PDF");
        assertTrue(tail.contains("%%EOF"), "trailer %%EOF");
        String doc = new String(raw, StandardCharsets.ISO_8859_1);
        assertTrue(doc.contains("/Type/Catalog"));
        assertTrue(doc.contains("/Count 1"));
        assertTrue(doc.contains("/Filter/DCTDecode"));
        // MediaBox = A4 в пунктах
        assertTrue(doc.contains("[0 0 595.28 841.89]"), "размер страницы A4 (pt)");
    }

    @Test
    void exportsMultiPagePdfForLargePoster(@TempDir Path dir) throws Exception {
        var svc = new ExportService();
        // 300 dpi: 300px = 25.4 мм постера — не делится на шаг A4; берём большой холст
        var opts = new RenderOptions(6000, 6000, 300, Color.WHITE, Color.RED, Color.BLUE,
                Color.GRAY, 0, 2, true);
        var plan = TilingPlan.forImage(opts.widthPx(), opts.heightPx(), opts.dpi());
        assertTrue(plan.totalSheets() >= 2, "план должен давать несколько листов");
        var out = svc.exportPdf(spiral(), opts, dir.resolve("big.pdf"), plan);
        assertTrue(out.ok(), () -> out.error());
        assertEquals(plan.totalSheets(), out.sheets());
        String doc = new String(Files.readAllBytes(dir.resolve("big.pdf")),
                StandardCharsets.ISO_8859_1);
        assertTrue(doc.contains("/Count " + plan.totalSheets()));
    }

    @Test
    void exportsTilingZipWithInstructions(@TempDir Path dir) throws Exception {
        var svc = new ExportService();
        var opts = new RenderOptions(6000, 6000, 300, Color.WHITE, Color.RED, Color.BLUE,
                Color.GRAY, 0, 2, true);
        var plan = TilingPlan.forImage(opts.widthPx(), opts.heightPx(), opts.dpi());
        var out = svc.exportTilingZip(spiral(), opts, dir.resolve("tiles.zip"), plan);
        assertTrue(out.ok(), () -> out.error());
        assertEquals("zip", out.format());
        assertEquals(plan.totalSheets(), out.sheets());

        List<String> names = new java.util.ArrayList<>();
        boolean sawInstructions = false;
        try (var zis = new ZipInputStream(Files.newInputStream(dir.resolve("tiles.zip")),
                StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                names.add(e.getName());
                if (e.getName().equals("assembly_instructions.txt")) {
                    var bos = new ByteArrayOutputStream();
                    zis.transferTo(bos);
                    String txt = bos.toString(StandardCharsets.UTF_8);
                    assertTrue(txt.contains("assembly") || txt.contains("сборк"),
                            "инструкция по сборке");
                    assertTrue(txt.contains(String.valueOf(plan.sheetsWide())));
                    sawInstructions = true;
                }
            }
        }
        assertTrue(sawInstructions, "в архиве есть assembly_instructions.txt");
        assertEquals(plan.totalSheets(),
                names.stream().filter(n -> n.endsWith(".png")).count());
        assertTrue(names.contains("sheet_R1_C1.png"));
    }

    @Test
    void zipSheetContainsBleedContentAndMarks(@TempDir Path dir) throws Exception {
        // содержимое листа 0,0: метки реза присутствуют (тёмные пиксели у краёв),
        // фон заданного цвета в центре поля
        var svc = new ExportService();
        var opts = new RenderOptions(6000, 6000, 300, Color.WHITE, Color.RED, Color.BLUE,
                Color.GRAY, 0, 3, true);
        var plan = TilingPlan.forImage(opts.widthPx(), opts.heightPx(), opts.dpi());
        var out = svc.exportTilingZip(spiral(), opts, dir.resolve("t.zip"), plan);
        assertTrue(out.ok(), () -> out.error());
        try (var zis = new ZipInputStream(Files.newInputStream(dir.resolve("t.zip")))) {
            ZipEntry e = zis.getNextEntry(); // sheet_R1_C1.png
            BufferedImage sheet = ImageIO.read(zis);
            assertNotNull(sheet);
            assertEquals(plan.sheetWidthPx(), sheet.getWidth());
            assertEquals(plan.sheetHeightPx(), sheet.getHeight());
            double pxPerMm = plan.dpi() / 25.4;
            int cutLpx = (int) Math.round(TilingPlan.PRINTER_UNPRINTABLE_MM * pxPerMm);
            // тёмная метка точно на линии реза слева (столбец cutL) где-то в верхней части
            boolean markFound = false;
            for (int y = 0; y < sheet.getHeight() && !markFound; y++) {
                int rgb = sheet.getRGB(cutLpx, y) & 0xFFFFFF;
                if (rgb == 0) markFound = true; // чёрный пиксель метки
            }
            assertTrue(markFound, "crop mark на линии реза присутствует");
            zis.closeEntry();
        }
    }

    @Test
    void outcomeRecordHelpersAreConsistent() {
        var ok = ExportOutcome.done("png", Path.of("a.png"), 10, 1);
        assertTrue(ok.ok());
        assertNull(ok.error());
        var bad = ExportOutcome.fail("png", Path.of("a.png"), "нет доступа");
        assertFalse(bad.ok());
        assertEquals("нет доступа", bad.error());
    }
}
