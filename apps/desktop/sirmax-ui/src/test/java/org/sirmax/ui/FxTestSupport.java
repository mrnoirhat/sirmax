// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;

/** Starts the JavaFX toolkit once per test JVM and runs work on the FX application thread. */
public final class FxTestSupport {

    private static boolean started;

    private FxTestSupport() {}

    public static synchronized void startToolkit() {
        if (started) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        await(latch);
        started = true;
    }

    public static <T> T onFxThread(Supplier<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(
                () -> {
                    try {
                        result.set(work.get());
                    } catch (RuntimeException e) {
                        failure.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
        await(latch);
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the JavaFX thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
