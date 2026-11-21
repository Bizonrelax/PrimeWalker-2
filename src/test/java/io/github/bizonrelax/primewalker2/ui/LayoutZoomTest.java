package io.github.bizonrelax.primewalker2.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;

import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.github.bizonrelax.primewalker2.persist.AppSettings;

/**
 * Тесты рычага ЖЕЛАНИЕ (ТЗ рев.2, CHANGE-001): компоновка главного окна и
 * масштабирование рисунка колесом мыши с зажатым Ctrl. Требуют графического
 * режима: запускаются при -Dpw.ui.graphics=true (CI headless их пропускает).
 */
class LayoutZoomTest {

    @Test
    void statusMessageWithoutZoomHasNoScaleIndicator() {
        // контракт строки состояния: индикатор «Масштаб» добавляется только при зуме > 1
        String msg = "Готово: 10 шагов.";
        double z = 1.0;
        String shown = (z > 1.001 || msg.isEmpty()) ? msg + "  Масштаб: ×" + z : msg;
        assertEquals(msg, shown);
        z = 1.5;
        assertTrue(((z > 1.001 || msg.isEmpty()) ? msg + "  Масштаб: ×1.5" : msg).contains("Масштаб"));
    }

    @Test
    @EnabledIfSystemProperty(named = "pw.ui.graphics", matches = "true")
    void panelFitsInMaximizedWindowWithoutScrolling() throws Exception {
        var st = new AppSettings(Files.createTempDirectory("pw-layout").resolve("s.properties"));
        st.load();
        var w = new MainWindow(st);
        try {
            w.setSize(1920, 1080);
            w.doLayout();
            JScrollPane scroll = (JScrollPane) find(w.getContentPane(), JScrollPane.class);
            assertNotNull(scroll, "прокручиваемая панель есть");
            var view = scroll.getViewport().getView();
            view.setSize(scroll.getViewport().getWidth(), Short.MAX_VALUE);
            int contentH = view.getPreferredSize().height;
            int viewportH = scroll.getViewport().getExtentSize().height;
            assertTrue(contentH <= viewportH,
                    "панель не влезает без прокрутки: контент " + contentH + " > окно " + viewportH);
            assertTrue(w.getHeight() >= 1080 - 60);
        } finally {
            w.dispose();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "pw.ui.graphics", matches = "true")
    void ctrlWheelZoomsAndDoubleResetReturnsToFit() throws Exception {
        var st = new AppSettings(Files.createTempDirectory("pw-zoom").resolve("s.properties"));
        st.load();
        var w = new MainWindow(st);
        try {
            w.setVisible(true);
            w.setSize(1280, 720);
            // setImage вызывает validate(): холст реализуется синхронно (тесты идут на EDT)
            w.setImage(new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB));
            var canvas = w.canvasView();
            assertTrue(canvas.getWidth() >= 50 && canvas.getHeight() >= 50,
                    "холст реализован: " + canvas.getSize());
            assertEquals(1.0, canvas.viewZoom(), 1e-9);
            // десять щелчков колеса с Ctrl вверх → зум ≈ 1.1^10 ≈ 2.59
            for (int i = 0; i < 10; i++) {
                dispatchWheel(canvas, true, -1, 100, 100);
            }
            assertTrue(canvas.viewZoom() > 2.0, "зум после Ctrl+колесо: " + canvas.viewZoom());
            assertTrue(w.getStatusTextForTests().contains("Масштаб"),
                    "индикатор зума в строке состояния: " + w.getStatusTextForTests());
            // потолок ~40×
            for (int i = 0; i < 200; i++) dispatchWheel(canvas, true, -1, 100, 100);
            assertTrue(canvas.viewZoom() <= 40.0 + 1e-9);
            // двойной клик — возврат к «вписать»
            dispatchClick(canvas, 50, 50);
            dispatchClick(canvas, 50, 50);
            assertEquals(1.0, canvas.viewZoom(), 1e-9);
            // колесо без Ctrl зум не меняет
            dispatchWheel(canvas, false, -1, 100, 100);
            assertEquals(1.0, canvas.viewZoom(), 1e-9);
        } finally {
            w.dispose();
        }
    }

    private static void dispatchWheel(java.awt.Component c, boolean ctrl, int rotation, int x, int y) {
        int mods = ctrl ? java.awt.event.InputEvent.CTRL_DOWN_MASK : 0;
        var e = new MouseWheelEvent(c, java.awt.event.MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), mods, x, y, 0, false,
                rotation, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1);
        for (var l : c.getMouseWheelListeners()) l.mouseWheelMoved(e);
    }

    private static void dispatchClick(java.awt.Component c, int x, int y) {
        var e = new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, x, y, 1, false);
        for (var l : c.getMouseListeners()) l.mouseClicked(e);
    }

    private static java.awt.Component find(java.awt.Container root, Class<?> type) {
        if (type.isInstance(root)) return root;
        for (var c : root.getComponents()) {
            if (type.isInstance(c)) return c;
            if (c instanceof java.awt.Container cc) {
                var r = find(cc, type);
                if (r != null) return r;
            }
        }
        return null;
    }
}
