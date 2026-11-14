package io.github.bizonrelax.primewalker2.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Рендеринг одного листа A4 постера (фрагмент 003): прямая отрисовка трассы
 * в координатах листа по плану TilingPlan + монтажные метки ТЗ —
 * crop marks, registration marks, нумерация "N из M", индикатор Top, butt guides.
 */
public final class SheetRenderer {

    /** Длина crop mark за линией реза, мм. */
    private static final double CROP_LEN_MM = 5.0;
    /** Границы печатной области листа, мм. */
    private static final double CUT_L = TilingPlan.PRINTER_UNPRINTABLE_MM;          // 5
    private static final double CUT_R = TilingPlan.A4_WIDTH_MM - TilingPlan.PRINTER_UNPRINTABLE_MM;  // 205
    private static final double CUT_T = TilingPlan.PRINTER_UNPRINTABLE_MM;          // 5
    private static final double CUT_B = TilingPlan.A4_HEIGHT_MM - TilingPlan.PRINTER_UNPRINTABLE_MM; // 292

    /**
     * Рендерит лист (col,row). Поза трассы: grid->мм листа по формуле плана,
     * затем mm->px при dpi плана. Метки рисуются поверх содержимого.
     *
     * @param path       точки трассы в координатах сетки
     * @param primeFlags флаги простоты (индексы как в path)
     * @param values     значения в точках для modulo-окраски или null
     * @param plan       план тесселяции
     * @param col        колонка листа, 0-based
     * @param row        строка листа, 0-based
     * @param opts       цвета/размеры рендеринга (dpi берётся из plan)
     * @return изображение листа sheetWidthPx x sheetHeightPx
     */
    public BufferedImage renderSheet(List<Double> path, boolean[] primeFlags, long[] values,
                                     TilingPlan plan, int col, int row, RenderOptions opts) {
        if (col < 0 || col >= plan.sheetsWide() || row < 0 || row >= plan.sheetsHigh()) {
            throw new IllegalArgumentException("Лист (" + col + "," + row + ") вне плана");
        }
        int dpi = plan.dpi();
        double pxPerMm = dpi / 25.4;
        int sw = plan.sheetWidthPx(), sh = plan.sheetHeightPx();
        var sheet = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(opts.background());
            g.fillRect(0, 0, sw, sh);

            // поза: пиксель постера X = gx*opts.scale? — здесь путь уже в пикселях постера
            // (grid-координаты домножаются вызывающим на scale), поэтому:
            Pose base = new Pose(1, 1,
                    (TilingPlan.PRINTABLE_MM - col * plan.stepXmm()) * pxPerMm,
                    (TilingPlan.PRINTABLE_MM - row * plan.stepYmm()) * pxPerMm);
            drawPath(g, path, primeFlags, values, base, opts);

            // --- метки ---
            double left = CUT_L * pxPerMm, right = CUT_R * pxPerMm;
            double top = CUT_T * pxPerMm, bottom = CUT_B * pxPerMm;
            double cropLen = CROP_LEN_MM * pxPerMm;
            g.setColor(markColor(opts));
            g.setStroke(new BasicStroke(Math.max(1, dpi / 150f)));
            drawCropMarks(g, left, right, top, bottom, cropLen);
            drawRegistrationMarks(g, left, right, top, bottom, cropLen);

            // butt guides: пунктирные линии склейки на границах перекрытия
            g.setColor(new Color(0x80_80_80));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[]{6f, 6f}, 0f));
            g.draw(new Line2D.Double(left, top, left, bottom));   // левая линия реза
            g.draw(new Line2D.Double(right, top, right, bottom)); // правая линия реза
            g.draw(new Line2D.Double(left, top, right, top));     // верхняя линия реза
            g.draw(new Line2D.Double(left, bottom, right, bottom)); // нижняя линия реза

            // нумерация листов "N из M" — левый верхний угол поля
            int number = row * plan.sheetsWide() + col + 1;
            g.setColor(markColor(opts));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, dpi / 10)));
            g.drawString(number + " из " + plan.totalSheets(), 2f, (float) (CUT_L * pxPerMm) - 3f);

            // индикатор ориентации Top — сверху по центру
            String topMark = "^ TOP";
            var fm = g.getFontMetrics();
            g.drawString(topMark, (sw - fm.stringWidth(topMark)) / 2f, (float) (CUT_L * pxPerMm) - 3f);
        } finally {
            g.dispose();
        }
        return sheet;
    }

    /**
     * Меточный оверлей (crop/registration метки, линии реза, нумерация, TOP) для
     * уже собранного изображения листа. Используется экспортом (фрагмент 004).
     */
    public BufferedImage renderMarks(BufferedImage content, TilingPlan plan,
                                     int col, int row, RenderOptions opts) {
        int dpi = plan.dpi();
        double pxPerMm = dpi / 25.4;
        int sw = content.getWidth(), sh = content.getHeight();
        var img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.drawImage(content, 0, 0, null);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double left = CUT_L * pxPerMm, right = CUT_R * pxPerMm;
            double top = CUT_T * pxPerMm, bottom = CUT_B * pxPerMm;
            double cropLen = CROP_LEN_MM * pxPerMm;
            g.setColor(markColor(opts));
            g.setStroke(new BasicStroke(Math.max(1, dpi / 150f)));
            drawCropMarks(g, left, right, top, bottom, cropLen);
            drawRegistrationMarks(g, left, right, top, bottom, cropLen);

            g.setColor(new Color(0x80_80_80));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[]{6f, 6f}, 0f));
            g.draw(new Line2D.Double(left, top, left, bottom));
            g.draw(new Line2D.Double(right, top, right, bottom));
            g.draw(new Line2D.Double(left, top, right, top));
            g.draw(new Line2D.Double(left, bottom, right, bottom));

            int number = row * plan.sheetsWide() + col + 1;
            g.setColor(markColor(opts));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, dpi / 10)));
            g.drawString(number + " из " + plan.totalSheets(), 2f, (float) (CUT_L * pxPerMm) - 3f);
            String topMark = "^ TOP";
            var fm = g.getFontMetrics();
            g.drawString(topMark, (sw - fm.stringWidth(topMark)) / 2f,
                    (float) (CUT_L * pxPerMm) - 3f);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Контрастный с фоном цвет меток. */
    private static Color markColor(RenderOptions opts) {
        var bg = opts.background();
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return lum > 128 ? Color.BLACK : Color.WHITE;
    }

    /** Отрисовка трассы в данной позе (общий код для листов и превью). */
    static void drawPath(Graphics2D g, List<Double> path, boolean[] primeFlags,
                         long[] values, Pose pose, RenderOptions opts) {
        if (opts.drawPath() && path.size() > 1) {
            g.setColor(opts.pathColor());
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < path.size(); i++) {
                Double a = path.get(i - 1), b = path.get(i);
                g.draw(new Line2D.Double(pose.px(a.x), pose.py(a.y), pose.px(b.x), pose.py(b.y)));
            }
        }
        int ps = opts.pointSizePx();
        for (int i = 0; i < path.size(); i++) {
            boolean isPrime = primeFlags != null && i < primeFlags.length && primeFlags[i];
            boolean isLast = i == path.size() - 1;
            Color c;
            if (isLast) {
                c = opts.endColor();
            } else if (isPrime) {
                c = (opts.modulo() > 0 && values != null && i < values.length)
                        ? RenderService.moduloColor(values[i], opts.modulo())
                        : opts.primeColor();
            } else {
                continue;
            }
            g.setColor(c);
            double x = pose.px(path.get(i).x), y = pose.py(path.get(i).y);
            g.fillRoundRect((int) Math.round(x - ps / 2.0), (int) Math.round(y - ps / 2.0),
                    ps, ps, ps, ps);
        }
    }

    /** Обрезные метки: две перпендикулярные линии в каждом углу области реза (ТЗ). */
    private static void drawCropMarks(Graphics2D g, double left, double right,
                                      double top, double bottom, double len) {
        for (double[] corner : new double[][]{{left, top}, {right, top}, {left, bottom}, {right, bottom}}) {
            double cx = corner[0], cy = corner[1];
            double sx = (cx == left) ? -1 : 1;
            double sy = (cy == top) ? -1 : 1;
            g.draw(new Line2D.Double(cx, cy, cx + sx * len, cy));
            g.draw(new Line2D.Double(cx, cy, cx, cy + sy * len));
        }
    }

    /** Контурные метки: крестик с кружком на середине каждой стороны области реза (ТЗ). */
    private static void drawRegistrationMarks(Graphics2D g, double left, double right,
                                              double top, double bottom, double len) {
        double midX = (left + right) / 2, midY = (top + bottom) / 2;
        double arm = len * 0.8, r = len * 0.35;
        for (double[] p : new double[][]{{midX, top}, {midX, bottom}, {left, midY}, {right, midY}}) {
            g.draw(new Line2D.Double(p[0] - arm, p[1], p[0] + arm, p[1]));
            g.draw(new Line2D.Double(p[0], p[1] - arm, p[0], p[1] + arm));
            g.draw(new Ellipse2D.Double(p[0] - r, p[1] - r, 2 * r, 2 * r));
        }
    }
}
