// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.document.PrinterProfile;

/**
 * Sends a rendered PDF to a physical printer (master prompt §59D).
 *
 * <p>Kept behind a port because printing is the one part of SIRMAX that cannot be verified in CI:
 * headless test runners have no printers. Tests exercise the rendering and the audit trail against a
 * recording double, and the real adapter is verified on a workstation during the Phase 13 print
 * audit.
 */
public interface DocumentPrinter {

    /** The print queues Windows currently offers, for the printer-profile screen. */
    List<String> availablePrinters();

    /** The system default queue, if the workstation has one. */
    Optional<String> defaultPrinter();

    /**
     * Print {@code pdf}.
     *
     * @param profile where and how; a silent profile must not open an OS dialog
     * @return {@code true} when the job was handed to the spooler; {@code false} when the operator
     *     cancelled the dialog. A hardware failure throws instead — a cancelled print is a normal
     *     outcome, a broken printer is not.
     */
    boolean print(byte[] pdf, PrinterProfile profile);
}
