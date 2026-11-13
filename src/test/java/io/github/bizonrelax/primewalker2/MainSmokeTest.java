package io.github.bizonrelax.primewalker2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Дымовой тест каркаса: класс существует, main запускается в headless-режиме. */
class MainSmokeTest {

    @Test
    void mainRunsInHeadlessMode() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
