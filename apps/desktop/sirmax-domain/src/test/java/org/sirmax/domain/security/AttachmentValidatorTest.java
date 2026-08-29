// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachmentValidatorTest {

    private static final long MAX = 25L * 1024 * 1024;

    private static byte[] header(int... bytes) {
        byte[] out = new byte[AttachmentValidator.HEADER_BYTES];
        for (int i = 0; i < bytes.length && i < out.length; i++) {
            out[i] = (byte) bytes[i];
        }
        return out;
    }

    private static byte[] pdf() {
        return header(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37);
    }

    private static byte[] jpeg() {
        return header(0xFF, 0xD8, 0xFF, 0xE0);
    }

    private static byte[] windowsExecutable() {
        return header(0x4D, 0x5A, 0x90, 0x00);
    }

    @Test
    void aRealPdfIsAccepted() {
        var verdict = AttachmentValidator.validate(pdf(), 120_000, MAX, "cedula.pdf");

        assertThat(verdict.accepted()).isTrue();
        assertThat(verdict.type()).contains(AttachmentValidator.AllowedType.PDF);
        assertThat(verdict.problem()).isEmpty();
    }

    /** The check that matters: content decides, never the name. */
    @Test
    void anExecutableRenamedToPdfIsRejected() {
        var verdict = AttachmentValidator.validate(windowsExecutable(), 90_000, MAX, "cedula.pdf");

        assertThat(verdict.accepted()).isFalse();
        assertThat(verdict.problem()).contains("attachment.unsupported_type");
    }

    @Test
    void anOfficeDocumentIsRejectedBecauseItCanCarryMacros() {
        // A .docx is a ZIP; the magic bytes are PK\003\004 and the allowlist has no entry for it.
        var verdict =
                AttachmentValidator.validate(
                        header(0x50, 0x4B, 0x03, 0x04), 40_000, MAX, "solicitud.docx");

        assertThat(verdict.accepted()).isFalse();
    }

    @Test
    void anExtensionThatDisagreesWithTheContentIsFlaggedButNotRefused() {
        var verdict = AttachmentValidator.validate(jpeg(), 200_000, MAX, "escaneo.pdf");

        assertThat(verdict.accepted()).isTrue();
        assertThat(verdict.type()).contains(AttachmentValidator.AllowedType.JPEG);
        assertThat(verdict.problem()).contains("attachment.extension_mismatch");
    }

    @Test
    void theUsualScannerAndPhoneExtensionsDoNotCountAsAMismatch() {
        assertThat(AttachmentValidator.validate(jpeg(), 1000, MAX, "foto.jpeg").problem()).isEmpty();
        assertThat(AttachmentValidator.validate(jpeg(), 1000, MAX, "FOTO.JPG").problem()).isEmpty();
    }

    @Test
    void anOversizedOrEmptyFileIsRejected() {
        assertThat(AttachmentValidator.validate(pdf(), MAX + 1, MAX, "a.pdf").problem())
                .contains("attachment.too_large");
        assertThat(AttachmentValidator.validate(pdf(), 0, MAX, "a.pdf").problem())
                .contains("attachment.empty");
    }

    @Test
    void everyRejectionGivesTheSameMessage() {
        // Telling an attacker which check failed is free reconnaissance, and the operator's next
        // step — ask for a PDF or a photo — is the same either way.
        var executable = AttachmentValidator.validate(windowsExecutable(), 1000, MAX, "x.exe");
        var script = AttachmentValidator.validate(header(0x23, 0x21, 0x2F), 1000, MAX, "x.sh");

        assertThat(executable.problem()).isEqualTo(script.problem());
    }
}
