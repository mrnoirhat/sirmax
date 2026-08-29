// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.domain.document.PaperFormat;

/**
 * Turns a frozen {@link DocumentSnapshot} into a real PDF (master prompt §59E — "real PDFs, not
 * screenshots").
 *
 * <p>The renderer reads nothing but the snapshot. That is what makes a reprint in 2031 identical to
 * the original: there is no path by which today's branding, today's fee table or today's citizen
 * record can reach the page.
 */
public interface DocumentRenderer {

    /**
     * Render to PDF bytes.
     *
     * @param markAsCopy stamp the output COPIA / REIMPRESIÓN (§59D)
     */
    byte[] render(DocumentSnapshot snapshot, PaperFormat format, boolean markAsCopy);
}
