// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

/** What a printed document is (master prompt §46, §59B). */
public enum DocumentKind {
    INVOICE,
    RECEIPT,
    CERTIFICATE,
    OFFICIAL_LETTER,
    PERMIT,
    /** A certified copy from the municipal register (§4). */
    REGISTRY_COPY,
    OTHER;

    /** {@code true} for documents whose reprint must be marked as a copy (§59D). */
    public boolean marksReprints() {
        return this == INVOICE || this == RECEIPT || this == CERTIFICATE || this == REGISTRY_COPY;
    }

    /** The paper a document of this kind defaults to. */
    public PaperFormat defaultFormat() {
        return this == RECEIPT ? PaperFormat.NARROW_80 : PaperFormat.LETTER;
    }
}
