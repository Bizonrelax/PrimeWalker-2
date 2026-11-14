package io.github.bizonrelax.primewalker2.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Сервис рендеринга: отрисовка трассы в BufferedImage через Graphics2D
 * (аналог QPainter из ТЗ, порт render.RenderService).
 */
public final class RenderService {

    /** Палитра модульной кодировки (ТЗ: остатки 0..N-1 своими цветами). */
    private static final Color[] MODULO_PALETTE = {
        new Color(0xE6_19_4C), // красный
        new Color(0x19_76_D2), // синий
        new Color(0x38_8E_3C), // зелёный
        new Color(0xF9_A8_25), // оранжевый
        new Color(0x7B_1F_a2), // фиолетовый
        new Color(0x00_AC_91), // бирюзовый
        new Color(0xC2_18_5B), // розовый
        new Color(0x82_77_CE), // индиго-лайт
        new Color(0x00_97_A7), // циан
        new Color(0xAF_B4_2B), // лаймовый
        new Color(0xD8_43_15), // терракот
        new Color(0x54_6E_7A), // серо-синий
        new Color(0xAD_14_57), // кармин
        new Color(0x55_8B_2F), // тёмно-зелёный
        new Color(0x6A_1B_9A), // тёмный фиолет
        new Color(0xE6_51_00), // тёмный оранжевый
    };

    /** Цвет остатка n по модулю modulo (0 &lt;= remainder &lt; modulo &lt;= 16). */
    public static Color moduloColor(long n, int modulo) {
        if (modulo <= 0) {
            throw new IllegalArgumentException("modulo должен быть > 0");
        }
        long r = Math.floorMod(n, modulo);
        return MODULO_PALETTE[(int) r % MODULO_PALETTE.length];
    }

    /**
     * Отрисовывает трассу (последовательность точек сетки) на холсте размера opts.
     * Простые точки рисуются цветом opts.primeColor (или палитрой modulo),
     * последняя точка — opts.endColor, линия трассы — opts.pathColor.
     *
     * @param path       точки трассы в координатах сетки
     * @param primeFlags флаги простоты, те же индексы, что и path
     * @param values     числовые значения в точках (для modulo-окраски); может быть null
     * @param pose       преобразование сетка-&gt;пиксели (для одной плитки передаётся сдвиг)
     * @param opts       параметры рендеринга
     * @return готовое изображение TYPE_INT_ARGB
     */
    public java.awt.image.BufferedImage renderImage(List<Point2D.Double> path,
                                                    boolean[] primeFlags, long[] values,
                                                    Pose pose, RenderOptions opts) {
        var img = new java.awt.image.BufferedImage(
                opts.widthPx(), opts.heightPx(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(opts.background());
            g.fillRect(0, 0, opts.widthPx(), opts.heightPx());

            if (opts.drawPath() && path.size() > 1) {
                g.setColor(opts.pathColor());
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 1; i < path.size(); i++) {
                    Point2D.Double a = path.get(i - 1), b = path.get(i);
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
                            ? moduloColor(values[i], opts.modulo())
                            : opts.primeColor();
                } else {
                    continue; // составные без modulo не рисуем яркими
                }
                g.setColor(c);
                double x = pose.px(path.get(i).x), y = pose.py(path.get(i).y);
                g.fillRoundRect((int) Math.round(x - ps / 2.0), (int) Math.round(y - ps / 2.0),
                        ps, ps, ps, ps);
            }
        } finally {
            g.dispose();
        }
        return img;
    }
}
