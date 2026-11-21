package io.github.bizonrelax.primewalker2.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import io.github.bizonrelax.primewalker2.core.BaseVisualizer;
import io.github.bizonrelax.primewalker2.core.VisualizerFactory;
import io.github.bizonrelax.primewalker2.persist.AppSettings;
import io.github.bizonrelax.primewalker2.persist.Colors;
import io.github.bizonrelax.primewalker2.persist.GlobalStats;
import io.github.bizonrelax.primewalker2.persist.ProjectManager;
import io.github.bizonrelax.primewalker2.persist.ProjectState;
import io.github.bizonrelax.primewalker2.render.Pose;
import io.github.bizonrelax.primewalker2.render.RenderOptions;
import io.github.bizonrelax.primewalker2.render.RenderService;

/**
 * Главное окно PrimeWalker-2 (порт ui.MainWindow, фрагмент 006; компоновка и просмотр —
 * ТЗ рев.2 / CHANGE-001): панель параметров слева фиксированной ширины ~320 px в две колонки
 * без прокрутки при развёрнутом окне 1920×1080, кнопки действий внизу панели, холст трассы
 * в центре на всю оставшуюся площадь, строка состояния на всю ширину окна, фоновый worker
 * с прогрессом и остановкой, экспорт через диалог, пост-отчёт об отрисовке, темы light/dark,
 * восстановление геометрии окна, сохранение настроек при закрытии.
 * Просмотр: Ctrl+колесо — зум к курсору, колесо — панорамирование, двойной клик — «вписать».
 */
public final class MainWindow extends JFrame {

    private static final int CANVAS_MAX_PX = 4096;
    private static final int MAX_STEPS_UI = 100_000_000; // ТЗ: ограничение числа шагов (PrimeGenerator.MAX_STEPS)
    private static final int PANEL_WIDTH = 320;          // ТЗ рев.2: ширина панели ~320 px
    private static final double ZOOM_STEP = 1.10;        // ТЗ рев.2: 10 % за щелчок колеса
    private static final double ZOOM_MAX = 40.0;         // ТЗ рев.2: до ~40× над подгонкой

    private final AppSettings st;
    private final GlobalStats stats;
    private final ProjectManager pm = new ProjectManager();
    private final Instant openedAt = Instant.now();

    private final JSpinner startNum = new JSpinner(new SpinnerNumberModel(1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L));
    private final JSpinner steps = new JSpinner(new SpinnerNumberModel(1000, 1, MAX_STEPS_UI, 100));
    private final JSpinner scale = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
    private final JComboBox<String> vizBox = new JComboBox<>(new String[]{"Ulam Spiral", "Jacob's Ladder", "Prime Walk"});
    private final JComboBox<String> moduloBox = new JComboBox<>(new String[]{"выкл", "3", "4", "5", "6", "7", "8"});
    private final JButton startColorBtn = colorButton(Color.RED);
    private final JButton endColorBtn = colorButton(Color.BLUE);
    private final JButton pathColorBtn = colorButton(Color.BLACK);
    private final JComboBox<String> bgBox = new JComboBox<>(new String[]{"#FFFFFF", "#808080", "#000000", "#000080", "#004000"});
    private final JComboBox<String> themeBox = new JComboBox<>(new String[]{"light", "dark"});
    private final JSpinner fontSpin = new JSpinner(new SpinnerNumberModel(12, 8, 16, 1));
    private final JButton generateBtn = new JButton("Начать генерацию");
    private final JButton stopBtn = new JButton("Остановить");
    private final JButton settingsBtn = new JButton("Настройки");
    private final JButton exportBtn = new JButton("Экспорт");
    private final JLabel progressLabel = new JLabel(" ");
    final Canvas canvas = new Canvas(); // package-private: доступ тестам компоновки/зума (LayoutZoomTest)

    private final RenderService renderer = new RenderService();
    private final AtomicBoolean cancelFlag = new AtomicBoolean();
    private Thread worker;
    private volatile Result result; // последний удачный результат генерации

    /** Данные последней генерации: трасса, флаги, значения, время расчёта и рендера. */
    public record Result(List<Point2D.Double> path, boolean[] primeFlags, long[] values,
                  double minX, double maxX, double minY, double maxY,
                  long calcMs, long renderMs, int widthPx, int heightPx) {}

    public MainWindow(AppSettings settings) {
        super("PrimeWalker-2");
        this.st = settings;
        this.stats = new GlobalStats(st);
        loadSettingsIntoForm();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { closeApp(); }
        });
        JPanel side = buildSidePanel();
        side.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        side.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
        // ТЗ рев.2: панель без прокрутки при развёрнутом окне; прокрутка — аварийный режим
        // только когда окно сильно уменьшено (минимальная высота панели меньше высоты окна).
        var scroll = new JScrollPane(side,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(PANEL_WIDTH, 100));
        scroll.setMinimumSize(new Dimension(PANEL_WIDTH, 100));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel center = new JPanel(new BorderLayout());
        center.add(canvas, BorderLayout.CENTER);
        add(scroll, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(progressLabel, BorderLayout.SOUTH); // строка состояния на всю ширину окна (ТЗ рев.2)
        setJMenuBar(buildMenu());
        installShortcuts();
        restoreGeometry();
        stats.sessionStarted();
        applyTheme();
        applyFontSize();
    }

    /** Показывает окно и запускает первую генерацию (вызывается из Main). */
    public void showAndRun() {
        setVisible(true);
        startGeneration();
    }

    // ---------- панель параметров ----------

    private JPanel buildSidePanel() {
        var p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        // ТЗ рев.2: двухколоночная панель — компактная раскладка, всё видно без прокрутки на 1080p
        p.add(group2("Параметры генерации",
                row("Начальное число", startNum),
                row("Число шагов", steps),
                row("Масштаб", scale)));
        p.add(group2("Настройки визуализации",
                row("Тип визуализации", vizBox),
                row("Модуль цвета", moduloBox),
                row("Цвет начальной точки", startColorBtn),
                row("Цвет конечной точки", endColorBtn),
                row("Цвет трассы", pathColorBtn),
                row("Цвет фона", bgBox),
                row("Тема", themeBox),
                row("Размер шрифта UI", fontSpin)));
        // Кнопки действий закреплены внизу панели (ТЗ рев.2) и остаются видимыми при любом размере окна:
        // блок «Настройки / Экспорт» — в SOUTH содержимого прокручиваемой панели, вне потока параметров.
        var aux = new JPanel(new GridLayout(1, 2, 6, 0));
        aux.add(settingsBtn);
        aux.add(exportBtn);
        var actions = new JPanel(new BorderLayout(0, 4));
        var mainBtns = group("Действия", generateBtn, stopBtn);
        mainBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainBtns.getPreferredSize().height));
        actions.add(mainBtns, BorderLayout.NORTH);
        actions.add(aux, BorderLayout.SOUTH);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        p.add(actions);
        p.add(Box.createVerticalGlue());

        generateBtn.addActionListener(e -> startGeneration());
        stopBtn.addActionListener(e -> cancelFlag.set(true));
        settingsBtn.addActionListener(e -> showSettingsDialog());
        exportBtn.addActionListener(e -> ExportDialog.open(this, st, stats, result));
        stopBtn.setEnabled(false);
        return p;
    }

    /** Группа в две колонки (ТЗ рев.2, требование 3): логическая группа сохраняется. */
    private static JPanel group2(String title, JComponent... rows) {
        var outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createTitledBorder(title));
        var grid = new JPanel(new GridLayout(0, 2, 6, 4));
        for (var r : rows) grid.add(r);
        outer.add(grid, BorderLayout.NORTH);
        outer.setAlignmentX(LEFT_ALIGNMENT);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, outer.getPreferredSize().height));
        return outer;
    }

    private static JPanel group(String title, JComponent... rows) {
        var g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setBorder(BorderFactory.createTitledBorder(title));
        for (var r : rows) {
            g.add(r);
            g.add(Box.createVerticalStrut(4));
        }
        g.setAlignmentX(LEFT_ALIGNMENT);
        return g;
    }

    private static JComponent row(String label, JComponent field) {
        var r = new JPanel(new BorderLayout(6, 0));
        var l = new JLabel(label);
        l.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 6));
        r.add(l, BorderLayout.NORTH);
        r.add(field, BorderLayout.CENTER);
        r.setAlignmentX(LEFT_ALIGNMENT);
        return r;
    }

    private static JButton colorButton(Color c) {
        var b = new JButton("     ");
        b.setBackground(c);
        b.setOpaque(true);
        return b;
    }

    private static Color swatch(JButton b) {
        Color c = b.getBackground();
        return c == null ? Color.BLACK : c;
    }

    private static void pickColor(JButton b) {
        Color c = JColorChooser.showDialog(null, "Выбор цвета", b.getBackground());
        if (c != null) b.setBackground(c);
    }

    // ---------- чтение/запись настроек ----------

    private void loadSettingsIntoForm() {
        startNum.setValue((long) st.startNumber());
        steps.setValue(st.stepCount());
        scale.setValue(st.scale());
        vizBox.setSelectedItem(st.visualizerType());
        moduloBox.setSelectedIndex(clampIdx(st.colorModulo() - 2, 0, moduloBox.getItemCount() - 1));
        startColorBtn.setBackground(parseColor(st.primeColor(), Color.RED));
        endColorBtn.setBackground(parseColor(st.endColor(), Color.BLUE));
        pathColorBtn.setBackground(parseColor(st.pathColor(), Color.BLACK));
        bgBox.setSelectedItem(st.backgroundColor().toUpperCase());
        themeBox.setSelectedItem(st.theme());
        fontSpin.setValue(st.fontSize());
    }

    private void saveFormIntoSettings() {
        st.put("ui/startNumber", ((Number) startNum.getValue()).intValue());
        st.put("ui/stepCount", steps.getValue());
        st.put("ui/scale", scale.getValue());
        st.put("ui/visualizerType", vizBox.getSelectedItem());
        st.put("ui/colorModulo", moduloIndex());
        st.put("ui/primeColor", hex(swatch(startColorBtn)));
        st.put("ui/endColor", hex(swatch(endColorBtn)));
        st.put("ui/pathColor", hex(swatch(pathColorBtn)));
        st.put("ui/backgroundColor", bgBox.getSelectedItem());
        st.put("ui/theme", themeBox.getSelectedItem());
        st.put("ui/fontSize", fontSpin.getValue());
        saveGeometry(); // ТЗ рев.2: геометрия + состояние развёрнутости окна
    }

    private int moduloIndex() {
        Object s = moduloBox.getSelectedItem();
        if (s == null || "выкл".equals(s)) return 0;
        try { return Integer.parseInt(s.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static int clampIdx(int v, int lo, int hi) { return Math.max(lo, Math.min(v, hi)); }

    static Color parseColor(String s, Color def) {
        try {
            if (s == null) return def;
            String t = s.trim();
            if (t.startsWith("#")) t = t.substring(1);
            if (t.length() == 6) return new Color(Integer.parseInt(t, 16));
        } catch (RuntimeException ignored) { }
        return def;
    }

    static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------- тема и шрифт ----------

    private void applyTheme() {
        boolean dark = "dark".equals(themeBox.getSelectedItem());
        var bg = dark ? new Color(0x1E_1E_1E) : Color.WHITE;
        var fg = dark ? Color.WHITE : Color.BLACK;
        getContentPane().setBackground(bg);
        for (var c : List.<JComponent>of(startNum, steps, scale, vizBox, moduloBox, bgBox, themeBox, fontSpin)) {
            c.setBackground(bg);
            c.setForeground(fg);
        }
        SwingUtilities.updateComponentTreeUI(this);
        for (var comp : getComponents()) comp.setForeground(fg);
        progressLabel.setForeground(fg);
    }

    private void applyFontSize() {
        int sz = ((Number) fontSpin.getValue()).intValue();
        var f = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, sz);
        setUIFont(f);
    }

    private void setUIFont(java.awt.Font f) {
        var defaults = javax.swing.UIManager.getDefaults();
        for (var key : defaults.keySet()) {
            Object v = defaults.get(key);
            if (v instanceof javax.swing.plaf.FontUIResource) defaults.put(key, new javax.swing.plaf.FontUIResource(f));
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    // ---------- геометрия окна ----------

    private void restoreGeometry() {
        int w = st.windowWidth(), h = st.windowHeight();
        // ТЗ рев.2: базовый размер не меньше 1280×720 — панель помещается без прокрутки
        w = Math.max(w, 1280);
        h = Math.max(h, 720);
        setSize(w, h);
        setMinimumSize(new Dimension(800, 600));
        if (st.windowMaximized()) setExtendedState(MAXIMIZED_BOTH);
        int x = st.windowX(), y = st.windowY();
        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }
    }

    /** Сохранение геометрии и состояния развёрнутости окна (ТЗ рев.2). */
    private void saveGeometry() {
        boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        st.put("window/maximized", maximized);
        if (!maximized) {
            st.put("window/x", getX());
            st.put("window/y", getY());
            st.put("window/width", getWidth());
            st.put("window/height", getHeight());
        }
    }

    // ---------- генерация в фоновом потоке ----------

    private void startGeneration() {
        if (worker != null && worker.isAlive()) return; // одна генерация за раз
        long start = ((Number) startNum.getValue()).longValue();
        int n = ((Number) steps.getValue()).intValue();
        double sc = ((Number) scale.getValue()).doubleValue();
        BaseVisualizer v = VisualizerFactory.byName((String) vizBox.getSelectedItem());
        int modulo = moduloIndex();
        Color bg = parseColor((String) bgBox.getSelectedItem(), Color.WHITE);
        Color pc = swatch(startColorBtn), ec = swatch(endColorBtn), tc = swatch(pathColorBtn);
        stats.algorithmUsed(v.displayName());
        saveFormIntoSettings();
        st.save();
        cancelFlag.set(false);
        stopBtn.setEnabled(true);
        generateBtn.setEnabled(false);
        updateStatus("Считаем простые и трассу…");
        worker = new Thread(() -> compute(v, start, n, sc, modulo, bg, pc, ec, tc), "pw-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void compute(BaseVisualizer v, long start, int n, double sc, int modulo,
                        Color bg, Color pc, Color ec, Color tc) {
        long t0 = System.currentTimeMillis();
        var res = v.render(start, n, sc);
        if (cancelFlag.get()) { finish(null, 0); return; }
        long calcMs = System.currentTimeMillis() - t0;
        int spanX = (int) Math.ceil(res.maxX() - res.minX()) + 2;
        int spanY = (int) Math.ceil(res.maxY() - res.minY()) + 2;
        int w = Math.min(CANVAS_MAX_PX, Math.max(spanX, 1));
        int h = Math.min(CANVAS_MAX_PX, Math.max(spanY, 1));
        double fit = Math.min((double) w / Math.max(spanX, 1), (double) h / Math.max(spanY, 1));
        double sx = fit, sy = fit;
        Pose pose = new Pose(sx, sy, (-res.minX() + 1) * sx, (-res.minY() + 1) * sy);
        var opts = new RenderOptions(w, h, 96, bg, pc, ec, tc, modulo, 2, true);
        long t1 = System.currentTimeMillis();
        var img = renderer.renderImage(res.path(), res.primeFlags(), res.values(), pose, opts);
        long renderMs = System.currentTimeMillis() - t1;
        Result r = new Result(res.path(), res.primeFlags(), res.values(),
                res.minX(), res.maxX(), res.minY(), res.maxY(), calcMs, renderMs, img.getWidth(), img.getHeight());
        imageCache = img;
        finish(r, renderMs);
    }

    private void finish(Result r, long renderMs) {
        SwingUtilities.invokeLater(() -> {
            stopBtn.setEnabled(false);
            generateBtn.setEnabled(true);
            if (r == null) {
                updateStatus("Остановлено.");
                return;
            }
            result = r;
            stats.addStepsDrawn(r.values().length);
            stats.imageGenerated(r.widthPx(), r.heightPx());
            saveFormIntoSettings();
            st.save();
            canvas.resetView(); // новая картинка — вид заново «вписать в окно» (ТЗ рев.2)
            canvas.repaint();
            updateStatus(String.format("Готово: %d шагов, расчёт %d мс, рендер %d мс.",
                    r.values().length, r.calcMs(), renderMs));
            if (!java.awt.GraphicsEnvironment.isHeadless()) showPostReport(r);
        });
    }

    // ---------- пост-отчёт об отрисовке ----------

    private void showPostReport(Result r) {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) >> 20;
        long maxMb = rt.maxMemory() >> 20;
        double mmPerPx = 25.4 / 96.0;
        double wMm = r.widthPx() * mmPerPx, hMm = r.heightPx() * mmPerPx;
        int cols = (int) Math.ceil(wMm / 195.0);
        int rows = (int) Math.ceil(hMm / 282.0);
        int sheets = Math.max(1, cols * rows);
        String rep = String.join("\n",
                "Отчёт об отрисовке PrimeWalker-2",
                "Размер рисунка: " + r.widthPx() + "×" + r.heightPx() + " px (" +
                        Math.round(wMm) + "×" + Math.round(hMm) + " мм)",
                "Листов A4 для печати: " + sheets,
                "Количество нарисованных шагов: " + r.values().length,
                "Время расчёта: " + r.calcMs() + " мс",
                "Время рендера: " + r.renderMs() + " мс",
                "RAM процесса: " + usedMb + " МБ из " + maxMb + " МБ",
                "Файл: ещё не экспортирован (кнопка «Экспорт»)");
        JOptionPane.showMessageDialog(this, rep, "Пост-отчёт", JOptionPane.INFORMATION_MESSAGE);
    }

    String reportText() {
        Result r = result;
        if (r == null) return "Нет выполненной генерации.";
        return String.join("\n",
                "Шаги: " + r.values().length,
                "Размер: " + r.widthPx() + "×" + r.heightPx(),
                "Расчёт: " + r.calcMs() + " мс, рендер: " + r.renderMs() + " мс");
    }

    // ---------- диалог настроек ----------

    private void showSettingsDialog() {
        var genAlgs = new JComboBox<>(new String[]{"Eratosthenes", "Segmented", "TrialDivision"});
        genAlgs.setSelectedItem(st.getString("settings/algorithm", "Eratosthenes"));
        var overlap = new JSpinner(new SpinnerNumberModel(10, 5, 10, 1));
        var reset = new JButton("Сбросить статистику");
        var ok = new JButton("Сохранить");
        var panel = new JPanel(new BorderLayout(6, 6));
        var grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.add(row("Алгоритм генерации простых", genAlgs));
        grid.add(row("Поле склейки листов, мм", overlap));
        panel.add(grid, BorderLayout.CENTER);
        var btns = new JPanel();
        btns.add(reset);
        btns.add(ok);
        panel.add(btns, BorderLayout.SOUTH);
        reset.addActionListener(e -> {
            stats.reset();
            st.save();
            JOptionPane.showMessageDialog(this, "Глобальная статистика сброшена.");
        });
        ok.addActionListener(e -> {
            st.put("settings/algorithm", genAlgs.getSelectedItem());
            st.put("export/overlapMm", overlap.getValue());
            st.save();
        });
        var dlg = new JDialog(this, "Настройки", true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(panel, BorderLayout.CENTER);
        dlg.pack();
        ok.addActionListener(e2 -> dlg.dispose());
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ---------- проекты и экспорт ----------

    void doSaveProject() {
        var ch = new JFileChooser();
        ch.setSelectedFile(new File("project.pwalk"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Result r = result;
        var ps = ProjectState.of(
                ((Number) startNum.getValue()).longValue(),
                ((Number) steps.getValue()).intValue(),
                ((Number) scale.getValue()).doubleValue(),
                (String) vizBox.getSelectedItem(),
                new Colors(hex(swatch(startColorBtn)), hex(swatch(endColorBtn)), hex(swatch(pathColorBtn))),
                moduloIndex(),
                r == null ? "" : "image-" + System.currentTimeMillis() + ".png");
        try {
            pm.saveProject(ch.getSelectedFile().toPath(), ps);
            st.put("project/latestSavePath", ch.getSelectedFile().getAbsolutePath());
            st.save();
            updateStatus("Проект сохранён: " + ch.getSelectedFile().getName());
        } catch (Exception ex) {
            showError("Не удалось сохранить проект", ex);
        }
    }

    void doOpenProject() {
        var ch = new JFileChooser();
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            ProjectState ps = pm.loadProject(ch.getSelectedFile().toPath());
            startNum.setValue(ps.initialNumber());
            steps.setValue(ps.stepCount());
            scale.setValue(ps.scale());
            vizBox.setSelectedItem(ps.visualizerType());
            moduloBox.setSelectedIndex(clampIdx(ps.colorModulo() == 0 ? 0 : ps.colorModulo() - 2, 0, moduloBox.getItemCount() - 1));
            startColorBtn.setBackground(parseColor(ps.colors().start(), Color.RED));
            endColorBtn.setBackground(parseColor(ps.colors().end(), Color.BLUE));
            pathColorBtn.setBackground(parseColor(ps.colors().path(), Color.BLACK));
            st.put("project/latestSavePath", ch.getSelectedFile().getAbsolutePath());
            st.save();
            updateStatus("Проект загружен: " + ch.getSelectedFile().getName());
            startGeneration();
        } catch (Exception ex) {
            showError("Не удалось открыть проект", ex);
        }
    }

    void doExport() {
        if (result == null) {
            JOptionPane.showMessageDialog(this, "Сначала выполните генерацию.", "Экспорт", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ExportDialog.open(this, st, stats, result);
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ": " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    // ---------- закрытие ----------

    private void closeApp() {
        if (worker != null && worker.isAlive()) cancelFlag.set(true);
        saveFormIntoSettings();
        stats.sessionEnded();
        st.save();
        dispose();
        System.exit(0);
    }

    // ---------- меню Файл / Помощь ----------

    private javax.swing.JMenuBar buildMenu() {
        var bar = new javax.swing.JMenuBar();
        var file = new javax.swing.JMenu("Файл");
        var miSave = new javax.swing.JMenuItem("Сохранить проект…");
        miSave.addActionListener(e -> doSaveProject());
        var miOpen = new javax.swing.JMenuItem("Открыть проект…");
        miOpen.addActionListener(e -> doOpenProject());
        var miExport = new javax.swing.JMenuItem("Экспорт…");
        miExport.addActionListener(e -> doExport());
        var miExit = new javax.swing.JMenuItem("Выход");
        miExit.addActionListener(e -> closeApp());
        file.add(miSave); file.add(miOpen); file.addSeparator(); file.add(miExport); file.addSeparator(); file.add(miExit);
        var help = new javax.swing.JMenu("Помощь");
        var miAbout = new javax.swing.JMenuItem("Содержание / О программе");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "PrimeWalker-2 — офлайн-визуализатор простых чисел: спираль Улама, лестница Якова, prime walk,\n" +
                "цветовая кодировка по модулю N, экспорт PNG/JPG/PDF/ZIP-плитки для печати A4.\n" +
                "Документация: README.md в папке программы.", "Помощь", JOptionPane.INFORMATION_MESSAGE));
        help.add(miAbout);
        bar.add(file); bar.add(help);
        return bar;
    }

    // ---------- горячие клавиши ----------

    private void installShortcuts() {
        startColorBtn.addActionListener(e -> pickColor(startColorBtn));
        endColorBtn.addActionListener(e -> pickColor(endColorBtn));
        pathColorBtn.addActionListener(e -> pickColor(pathColorBtn));
        themeBox.addActionListener(e -> { applyTheme(); });
        fontSpin.addChangeListener(e -> applyFontSize());
    }

    // ---------- холст ----------

    /**
     * Холст трассы (ТЗ рев.2 / CHANGE-001): Ctrl+колесо — масштабирование к курсору
     * (10 % за щелчок, от подгонки до ~40×), колесо без Ctrl — панорамирование,
     * двойной клик — возврат к «вписать в окно», коэффициент зума — в строке состояния.
     */
    final class Canvas extends JPanel {

        private double zoom = 1.0;          // множитель над базовой подгонкой
        private double panX, panY;          // смещение центра картинки относительно центра холста
        private Point dragStart;            // точка начала протяжки мышью
        private double dragPanX, dragPanY;

        Canvas() {
            setOpaque(true);
            setPreferredSize(new Dimension(600, 400));
            addMouseWheelListener(this::onWheel);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) resetView();
                }
                @Override public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint(); dragPanX = panX; dragPanY = panY;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart == null) return;
                    panX = dragPanX + (e.getX() - dragStart.x);
                    panY = dragPanY + (e.getY() - dragStart.y);
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }

        /** Сброс зума и панорамы — вид «вписать в окно». */
        void resetView() { zoom = 1.0; panX = 0; panY = 0; updateStatus(lastMessage); }

        private void onWheel(MouseWheelEvent e) {
            var img = imageCache;
            if (img == null || getWidth() < 10 || getHeight() < 10) return; // холст ещё не реализован
            if (e.isControlDown()) {
                // ТЗ: масштабирование рисунка колесом мыши с зажатым Ctrl — к точке под курсором
                double factor = e.getWheelRotation() < 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
                double nz = Math.max(1.0, Math.min(ZOOM_MAX, zoom * factor));
                if (nz != zoom) {
                    double cx = getWidth() / 2.0 + panX, cy = getHeight() / 2.0 + panY;
                    double dx = e.getX() - cx, dy = e.getY() - cy;
                    double k = nz / zoom;
                    panX = cx + dx / k - getWidth() / 2.0; // точка под курсором остаётся на месте
                    panY = cy + dy / k - getHeight() / 2.0;
                    zoom = nz;
                    updateStatus(lastMessage);
                    repaint();
                }
            } else {
                // колесо без Ctrl — панорамирование по вертикали, Shift+колесо — по горизонтали
                int d = e.getUnitsToScroll();
                if (e.isShiftDown()) panX -= d; else panY -= d;
                clampPan(img);
                repaint();
            }
        }

        private void clampPan(java.awt.image.BufferedImage img) {
            double[] f = fittedScale(img);
            double dw = img.getWidth() * f[0] * zoom, dh = img.getHeight() * f[1] * zoom;
            double maxX = Math.max(0, (dw - getWidth()) / 2.0) + 64;
            double maxY = Math.max(0, (dh - getHeight()) / 2.0) + 64;
            panX = Math.max(-maxX, Math.min(maxX, panX));
            panY = Math.max(-maxY, Math.min(maxY, panY));
        }

        private double[] fittedScale(java.awt.image.BufferedImage img) {
            double fx = (double) getWidth() / img.getWidth();
            double fy = (double) getHeight() / img.getHeight();
            double fit = Math.min(1.0, Math.min(fx, fy)); // не увеличиваем выше подгонки
            return new double[]{fit, fit};
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            var g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Result r = result;
            if (r == null) {
                g.setColor(Color.GRAY);
                g.drawString("Нет изображения — нажмите «Начать генерацию»", 20, 30);
                return;
            }
            var img = imageCache;
            if (img == null) {
                g.setColor(parseColor((String) bgBox.getSelectedItem(), Color.WHITE));
                g.fillRect(0, 0, getWidth(), getHeight());
                return;
            }
            double[] f = fittedScale(img);
            double sx = f[0] * zoom, sy = f[1] * zoom;
            int x = (int) Math.round(getWidth() / 2.0 - img.getWidth() * sx / 2.0 + panX);
            int y = (int) Math.round(getHeight() / 2.0 - img.getHeight() * sy / 2.0 + panY);
            g.drawImage(img, new AffineTransform(sx, 0, 0, sy, x, y), null);
        }

        /** Текущий коэффициент зума для строки состояния (ТЗ: индикатор масштаба). */
        double viewZoom() { return zoom; }
    }

    private volatile java.awt.image.BufferedImage imageCache;
    private String lastMessage = " ";

    /** Строка состояния на всю ширину окна; показывает зум (ТЗ рев.2). */
    void updateStatus(String msg) {
        lastMessage = msg;
        double z = canvas.viewZoom();
        progressLabel.setText(z > 1.001 || msg.isEmpty()
                ? msg + "  Масштаб: ×" + String.format("%.1f", z)
                : msg);
    }

    /** Панорамирование и зум холста — доступны тестам компоновки/просмотра. */
    Canvas canvasView() { return canvas; }

    /** Текст строки состояния — для тестов индикатора масштаба. */
    String getStatusTextForTests() { return progressLabel.getText(); }

    public java.awt.image.BufferedImage currentImage() { return imageCache; }

    public Result currentResult() { return result; }

    AppSettings settings() { return st; }

    void setImage(java.awt.image.BufferedImage img) {
        imageCache = img;
        canvas.validate(); // синхронная раскладка: реализация холста для тестов и корректного зума к курсору
        canvas.repaint();
    }
}
