package io.github.bizonrelax.primewalker2.export;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Минимальный собственный PDF-writer без внешних библиотек (решение 2 AUDIT.md:
 * полная сертификация PDF/X-4 на стеке протокола недоступна — формируется
 * максимально близкий печатно-готовый документ). Единицы MediaBox — точки
 * (1 pt = 1/72"), содержимое страницы — JPEG-изображение, растянутое на всю
 * страницу (DCTDecode, без пережатия при записи).
 */
final class PdfWriter {

    private record Page(double wPt, double hPt, byte[] jpeg, int imgW, int imgH) {}

    private final List<Page> pages = new ArrayList<>();
    private long written;

    /** Добавляет страницу размером wPt x hPt точек с изображением на всю площадь. */
    void addPage(double wPt, double hPt, BufferedImage img) throws IOException {
        var rgb = toRgb(img);
        var bos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(rgb, "jpg", bos);
        pages.add(new Page(wPt, hPt, bos.toByteArray(), rgb.getWidth(), rgb.getHeight()));
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        var dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = dst.createGraphics();
        g.drawImage(src, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();
        return dst;
    }

    private static String num(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    /** Записывает полный документ (заголовок, объекты, xref, trailer) в out. */
    void write(OutputStream out) throws IOException {
        written = 0;
        int n = pages.size();
        int total = 3 + 3 * n; // catalog, pages, на страницу: page, content, image
        long[] off = new long[total];
        var buf = new ByteArrayOutputStream();
        w(buf, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");
        off[1] = mark(buf, out);
        w(buf, "1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n");
        off[2] = mark(buf, out);
        var kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) kids.append(' ');
            kids.append(pageObj(i)).append(" 0 R");
        }
        w(buf, "2 0 obj\n<</Type/Pages/Kids[" + kids + "]/Count " + n + ">>\nendobj\n");
        for (int i = 0; i < n; i++) {
            Page p = pages.get(i);
            byte[] content = ("q " + num(p.wPt()) + " 0 0 " + num(p.hPt()) + " 0 0 cm /Im0 Do Q")
                    .getBytes(StandardCharsets.US_ASCII);
            off[pageObj(i)] = mark(buf, out);
            w(buf, pageObj(i) + " 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 "
                    + num(p.wPt()) + " " + num(p.hPt()) + "]/Resources<</XObject<</Im0 "
                    + imgObj(i) + " 0 R>>>>/Contents " + contObj(i) + " 0 R>>\nendobj\n");
            off[contObj(i)] = mark(buf, out);
            w(buf, contObj(i) + " 0 obj\n<</Length " + content.length + ">>\nstream\n");
            flush(buf, out);
            out.write(content);
            written += content.length;
            w(buf, "\nendstream\nendobj\n");
            off[imgObj(i)] = mark(buf, out);
            w(buf, imgObj(i) + " 0 obj\n<</Type/XObject/Subtype/Image/Width " + p.imgW()
                    + "/Height " + p.imgH() + "/ColorSpace /DeviceRGB/BitsPerComponent 8"
                    + "/Filter/DCTDecode/Length " + p.jpeg().length + ">>\nstream\n");
            flush(buf, out);
            out.write(p.jpeg());
            written += p.jpeg().length;
            w(buf, "\nendstream\nendobj\n");
        }
        long startx = mark(buf, out);
        var x = new StringBuilder("xref\n0 " + total + "\n0000000000 65535 f \n");
        for (int i = 1; i < total; i++) {
            x.append(String.format("%010d 00000 n  ", off[i])).append('\n');
        }
        x.append("trailer\n<</Size ").append(total).append("/Root 1 0 R>>\nstartxref\n")
         .append(startx).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static int pageObj(int i) { return 3 + 3 * i; }
    private static int contObj(int i) { return 4 + 3 * i; }
    private static int imgObj(int i) { return 5 + 3 * i; }

    private void w(ByteArrayOutputStream buf, String s) {
        buf.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private long mark(ByteArrayOutputStream buf, OutputStream out) throws IOException {
        flush(buf, out);
        return written;
    }

    private void flush(ByteArrayOutputStream buf, OutputStream out) throws IOException {
        if (buf.size() > 0) {
            byte[] b = buf.toByteArray();
            out.write(b);
            written += b.length;
            buf.reset();
        }
    }
}
