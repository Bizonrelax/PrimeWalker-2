package io.github.bizonrelax.primewalker2;

import javax.swing.SwingUtilities;

/**
 * Точка входа PrimeWalker-2 — офлайн-приложения визуализации простых чисел.
 *
 * Каркас фрагмента 000. Полное окно (ui.MainWindow) подключается фрагментом 006:
 * тогда тело run() заменяется на создание и показ MainWindow, а headless-режим
 * сохраняет CLI-проверку работоспособности сборки.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean headless = java.awt.GraphicsEnvironment.isHeadless();
        if (headless) {
            // Headless-запуск (CI, сервер без дисплея): печатаем приветствие и выходим 0.
            System.out.println("PrimeWalker-2 v1.0.0: graphical environment is not available; use the portable EXE build on Windows.");
            return;
        }
        SwingUtilities.invokeLater(Main::run);
    }

    private static void run() {
        // TODO(фрагмент 006): new ui.MainWindow(persist.AppSettings.load()).showAndRun();
        System.out.println("PrimeWalker-2: UI is provided by fragment 006.");
    }
}
