// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

/**
 * The paper a document is laid out for (master prompt §59B).
 *
 * <p>Two are mandatory: {@link #LETTER} for the office invoice and one of the narrow widths for the
 * counter receipt printer. {@link #A4} exists because §59B.2 requires the template system to accept
 * it later "without changing the billing domain" — proving that is easier than promising it.
 *
 * <p>Dimensions are in PostScript points (1/72 inch), which is what every PDF renderer wants. Narrow
 * formats have no fixed height: receipt paper is continuous, so the renderer computes the page
 * length from the content.
 */
public enum PaperFormat {
    /** 8.5 × 11 in — the office invoice (§59B.2). */
    LETTER(612f, 792f),
    A4(595.28f, 841.89f),
    /** 58 mm thermal/impact roll — the narrowest common counter printer. */
    NARROW_58(164.41f, 0f),
    /** 80 mm roll — the usual Dominican counter receipt printer. */
    NARROW_80(226.77f, 0f);

    private final float widthPoints;
    private final float heightPoints;

    PaperFormat(float widthPoints, float heightPoints) {
        this.widthPoints = widthPoints;
        this.heightPoints = heightPoints;
    }

    public float widthPoints() {
        return widthPoints;
    }

    /** Fixed page height, or {@code 0} for continuous roll paper. */
    public float heightPoints() {
        return heightPoints;
    }

    /** {@code true} for continuous roll paper, whose page grows with the content. */
    public boolean isContinuous() {
        return heightPoints == 0f;
    }

    /**
     * {@code true} for the narrow counter formats.
     *
     * <p>These print on low-resolution black-and-white impact hardware, so the renderer drops
     * colour, logos and table rules for them and leans on plain monospaced text (§59B.1).
     */
    public boolean isNarrow() {
        return this == NARROW_58 || this == NARROW_80;
    }

    /** Roughly how many monospaced characters fit across the printable width. */
    public int monospaceColumns() {
        return switch (this) {
            case NARROW_58 -> 32;
            case NARROW_80 -> 42;
            case LETTER, A4 -> 80;
        };
    }
}
