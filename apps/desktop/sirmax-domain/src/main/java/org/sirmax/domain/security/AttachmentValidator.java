// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Decides whether a file may be attached to a case (master prompt §43 — safe file validation).
 *
 * <p>The check that matters is the <b>magic bytes</b>, not the extension. A citizen who renames
 * {@code virus.exe} to {@code cedula.pdf} defeats an extension check entirely, and municipal staff
 * accept whatever is handed to them at the counter. So the content has to say what it is.
 *
 * <p>The allowlist is what a municipality actually receives: scans and photographs of documents. It
 * excludes everything executable, every archive, and every format with a scripting engine — Office
 * documents included. A citizen bringing a {@code .docx} is asked for a PDF or a photo, which is a
 * small friction next to letting macro-bearing files into a municipal file store.
 */
public final class AttachmentValidator {

    /** What a municipal counter legitimately receives. */
    public enum AllowedType {
        PDF("application/pdf", ".pdf", new byte[] {0x25, 0x50, 0x44, 0x46}),
        JPEG("image/jpeg", ".jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG(
                "image/png",
                ".png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        TIFF_LE("image/tiff", ".tif", new byte[] {0x49, 0x49, 0x2A, 0x00}),
        TIFF_BE("image/tiff", ".tif", new byte[] {0x4D, 0x4D, 0x00, 0x2A});

        private final String contentType;
        private final String extension;
        private final byte[] magic;

        AllowedType(String contentType, String extension, byte[] magic) {
            this.contentType = contentType;
            this.extension = extension;
            this.magic = magic;
        }

        public String contentType() {
            return contentType;
        }

        public String extension() {
            return extension;
        }

        boolean matches(byte[] header) {
            if (header.length < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if (header[i] != magic[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @param problem an i18n key when the file is rejected, empty when it is accepted
     */
    public record Verdict(boolean accepted, Optional<AllowedType> type, Optional<String> problem) {

        static Verdict accepted(AllowedType type) {
            return new Verdict(true, Optional.of(type), Optional.empty());
        }

        static Verdict rejected(String problem) {
            return new Verdict(false, Optional.empty(), Optional.of(problem));
        }
    }

    /** Longest magic sequence above; the caller need read no more than this from the file. */
    public static final int HEADER_BYTES = 8;

    private AttachmentValidator() {}

    /**
     * Validate a file by its first bytes, its size and its name.
     *
     * @param header the file's first {@link #HEADER_BYTES} bytes
     * @param sizeBytes the whole file's size
     * @param maxBytes the policy limit
     * @param fileName as supplied; used only to warn about a mismatch, never to decide
     */
    public static Verdict validate(byte[] header, long sizeBytes, long maxBytes, String fileName) {
        Objects.requireNonNull(header, "header");
        if (sizeBytes <= 0) {
            return Verdict.rejected("attachment.empty");
        }
        if (sizeBytes > maxBytes) {
            return Verdict.rejected("attachment.too_large");
        }

        Optional<AllowedType> detected =
                List.of(AllowedType.values()).stream().filter(t -> t.matches(header)).findFirst();
        if (detected.isEmpty()) {
            // Deliberately one message for every rejection: telling an attacker *which* check
            // failed is free reconnaissance, and the operator's next step is the same either way.
            return Verdict.rejected("attachment.unsupported_type");
        }

        // The extension disagreeing with the content is not fatal — a scanner that writes .jpeg or
        // a phone that writes .JPG is common — but it is worth surfacing, because it is also what a
        // renamed executable looks like right before the magic check catches it.
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (!name.isEmpty() && !name.endsWith(detected.get().extension())
                && !isKnownAlias(name, detected.get())) {
            return new Verdict(
                    true, detected, Optional.of("attachment.extension_mismatch"));
        }
        return Verdict.accepted(detected.get());
    }

    private static boolean isKnownAlias(String name, AllowedType type) {
        return switch (type) {
            case JPEG -> name.endsWith(".jpeg") || name.endsWith(".jpe");
            case TIFF_LE, TIFF_BE -> name.endsWith(".tiff");
            case PDF, PNG -> false;
        };
    }
}
