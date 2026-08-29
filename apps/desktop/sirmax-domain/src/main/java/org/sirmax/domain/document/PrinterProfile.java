// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A configured printer at a workstation (master prompt §59D).
 *
 * <p>Configured once so the counter never sees a print dialog again: §59D is explicit that pressing
 * <em>Imprimir</em> must produce a real invoice without the operator exporting anything by hand.
 *
 * @param printerName the Windows print queue; empty means the system default printer
 * @param workstation the hostname this profile belongs to; empty means "any", for a single-machine
 *     office where naming the host would be noise
 * @param silent skip the OS dialog — right for a receipt printer, wrong for shared office paper
 */
public record PrinterProfile(
        String id,
        String name,
        Optional<String> printerName,
        PaperFormat paperFormat,
        Optional<String> workstation,
        boolean isDefault,
        int copies,
        boolean silent,
        Instant createdAt,
        Instant updatedAt) {

    public PrinterProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(paperFormat, "paperFormat");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        name = name.strip();
        printerName = orEmpty(printerName);
        workstation = orEmpty(workstation);
        if (copies < 1 || copies > 5) {
            throw new IllegalArgumentException("copies must be between 1 and 5");
        }
    }

    public static PrinterProfile of(String id, String name, PaperFormat format, Instant now) {
        return new PrinterProfile(
                id,
                name,
                Optional.empty(),
                format,
                Optional.empty(),
                false,
                1,
                format.isNarrow(), // a receipt printer should never stop to ask
                now,
                now);
    }

    /** {@code true} when this profile applies on {@code host}. */
    public boolean appliesTo(String host) {
        return workstation.isEmpty() || workstation.get().equalsIgnoreCase(host);
    }

    private static Optional<String> orEmpty(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
