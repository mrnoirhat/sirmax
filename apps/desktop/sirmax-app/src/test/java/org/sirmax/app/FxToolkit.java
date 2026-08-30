// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;

/**
 * Starts the JavaFX toolkit once per JVM and runs work on its thread.
 *
 * <p>The toolkit is a JVM-wide singleton, but a {@code static boolean} guard is per class — so two
 * integration tests each guarding their own startup work in isolation and fail the moment they run
 * in the same fork, which is exactly how Gradle runs them. The flag has to live in one place, and
 * this is it.
 */
final class FxToolkit {

    private static boolean started;

    private FxToolkit() {}

    static synchronized void start() {
        if (started) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        await(latch);
        started = true;
    }

    /** Runs {@code work} on the JavaFX thread and returns its result, rethrowing any failure. */
    static <T> T onFxThread(Supplier<T> work) {
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

    static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for the JavaFX thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
