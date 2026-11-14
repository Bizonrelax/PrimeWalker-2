package io.github.bizonrelax.primewalker2.ui;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import io.github.bizonrelax.primewalker2.persist.AppSettings;
import io.github.bizonrelax.primewalker2.persist.GlobalStats;

/** Диалог экспорта: выбор PNG/JPG/PDF/ZIP-плитки, запись в историю экспортов и статистику форматов. */
public final class ExportDialog {

    private ExportDialog() {}

    public static void open(MainWindow owner, AppSettings st, GlobalStats stats, MainWindow.Result result) {
        BufferedImage img = owner.currentImage();
        if (img == null) {
            JOptionPane.showMessageDialog(owner, "Нет изображения для экспорта.", "Экспорт", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var ch = new JFileChooser();
        ch.setSelectedFile(new java.io.File("primewalker.png"));
        if (ch.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        Path file = ch.getSelectedFile().toPath();
        String name = file.getFileName().toString().toLowerCase();
        ExportGateway gw = ExportGateway.Registry.get();
        if (gw == null) {
            JOptionPane.showMessageDialog(owner,
                    "Модуль экспорта ещё не подключён (ожидается фрагмент 004).", "Экспорт",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ExportGateway.Outcome oc = switch (name) {
                case String s when s.endsWith(".jpg") || s.endsWith(".jpeg") -> gw.exportJpg(img, file, 0.92f);
                case String s when s.endsWith(".pdf") -> gw.exportPdf(img, file, true, 3.0);
                case String s when s.endsWith(".zip") -> gw.exportZipTiles(img, file, 10.0);
                default -> gw.exportPng(img, file);
            };
            String fmt = name.replaceAll(".*\\.([a-z]+)$", "$1").toUpperCase();
            stats.formatUsed(fmt);
            st.addExport(new AppSettings.ExportRecord(Instant.now().toString(), file.toString(),
                    img.getWidth(), img.getHeight(), Math.round(oc.bytes() / 1048576.0 * 100) / 100.0));
            st.save();
            JOptionPane.showMessageDialog(owner,
                    "Экспортировано: " + file + " (" + oc.bytes() + " байт, листов: " + oc.sheets() + ").",
                    "Экспорт", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Ошибка экспорта: " + ex.getMessage(),
                    "Экспорт", JOptionPane.ERROR_MESSAGE);
        }
    }
}
