// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.print;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.shared.Money;

/**
 * Lays a document out as monospaced lines for a narrow counter printer (master prompt §59B.1).
 *
 * <p>Separate from the PDF renderer, and free of PDFBox, because this is where the hard part is: it
 * has to stay legible on a 58 mm roll at 180 dpi in black and white. That rules out rules, tints and
 * proportional type, so everything is done with character columns — which also makes it directly
 * testable, without parsing a PDF.
 *
 * <p>Long concepts wrap rather than truncate. A citizen holding a receipt that says "Certificación
 * de uso de su…" cannot tell what they paid for.
 */
final class NarrowReceiptLayout {

    /** Width of the amount column; RD$ plus up to 999,999.99 fits in 14. */
    private static final int AMOUNT_WIDTH = 14;

    private final int columns;
    private final DateTimeFormatter stamp;

    NarrowReceiptLayout(int columns, DateTimeFormatter stamp) {
        this.columns = columns;
        this.stamp = stamp;
    }

    List<String> lay(DocumentSnapshot s, boolean markAsCopy) {
        List<String> out = new ArrayList<>();

        if (markAsCopy) {
            out.add(centre("*** COPIA ***"));
            out.add("");
        }

        // ── header ──
        DocumentSnapshot.Institution institution = s.institution();
        out.addAll(wrapCentred(institution.name()));
        institution.municipality().ifPresent(m -> out.addAll(wrapCentred(m)));
        institution.address().ifPresent(a -> out.addAll(wrapCentred(a)));
        institution.phone().ifPresent(p -> out.add(centre("Tel. " + p)));
        institution.legalIdentifier().ifPresent(id -> out.add(centre("RNC " + id)));
        out.add(separator());

        // ── identity ──
        addPair(out, "No.", s.documentNumber());
        addPair(out, "Fecha", stamp.format(s.issuedAt()));
        s.issuedByName().ifPresent(name -> addPair(out, "Atendió", name));
        s.reference().ifPresent(reference -> addPair(out, "Ref.", reference));
        out.add(separator());

        // ── customer ──
        out.addAll(wrap(s.customer().name()));
        s.customer()
                .identificationNumber()
                .ifPresent(
                        number ->
                                out.add(
                                        s.customer().identificationType().orElse("Cédula")
                                                + ": "
                                                + number));
        out.add(separator());

        // ── detail ──
        for (DocumentSnapshot.Line line : s.lines()) {
            out.addAll(wrap(line.concept()));
            // "2 x RD$ 250.00" on its own line keeps the arithmetic visible on 32 columns.
            String quantity =
                    line.quantity() == 1
                            ? ""
                            : line.quantity() + " x " + PdfDocumentRenderer.amount(line.unitPrice());
            if (!quantity.isEmpty()) {
                out.add("  " + quantity);
            }
            if (!line.discount().isZero()) {
                addPair(out, "  Descuento", "-" + PdfDocumentRenderer.amount(line.discount()));
            }
            addPair(out, "", PdfDocumentRenderer.amount(line.lineTotal()));
        }
        out.add(separator());

        // ── totals ──
        DocumentSnapshot.Totals totals = s.totals();
        addPair(out, "Subtotal", PdfDocumentRenderer.amount(totals.subtotal()));
        if (!totals.discount().isZero()) {
            addPair(out, "Descuento", PdfDocumentRenderer.amount(totals.discount()));
        }
        if (!totals.surcharge().isZero()) {
            addPair(out, "Cargos", PdfDocumentRenderer.amount(totals.surcharge()));
        }
        addPair(out, "TOTAL", PdfDocumentRenderer.amount(totals.total()));
        addPair(out, "Pagado", PdfDocumentRenderer.amount(totals.paid()));
        if (!totals.balance().isZero()) {
            addPair(out, "PENDIENTE", PdfDocumentRenderer.amount(totals.balance()));
        }

        // ── payment ──
        s.payment()
                .ifPresent(
                        payment -> {
                            out.add(separator());
                            addPair(out, "Forma", payment.method());
                            payment.tendered()
                                    .ifPresent(
                                            t ->
                                                    addPair(out, "Recibido",
                                                                    PdfDocumentRenderer.amount(t)));
                            payment.change()
                                    .filter(Money::isPositive)
                                    .ifPresent(
                                            ch ->
                                                    addPair(out, "CAMBIO",
                                                                    PdfDocumentRenderer.amount(ch)));
                            payment.reference()
                                    .ifPresent(reference -> addPair(out, "Ref.", reference));
                        });

        // ── footer ──
        out.add(separator());
        out.add(centre("Verificación"));
        out.add(centre(s.verificationCode()));
        s.footerNote().ifPresent(note -> out.addAll(wrapCentred(note)));
        institution.website().ifPresent(site -> out.add(centre(site)));
        out.add("");
        return List.copyOf(out);
    }

    /**
     * Adds {@code label} left with {@code value} right-aligned against the right edge.
     *
     * <p>When the two cannot share a line — a 15-character document number on a 32-column roll —
     * the label goes on its own line and the value right-aligns below it, rather than overflowing.
     * Truncating instead would cost the citizen the very number they need to quote.
     */
    private void addPair(List<String> out, String label, String value) {
        if (label.length() + value.length() + 1 <= columns) {
            int labelRoom = columns - Math.max(AMOUNT_WIDTH, value.length());
            out.add(
                    String.format(
                            "%-" + labelRoom + "s%" + (columns - labelRoom) + "s", label, value));
            return;
        }
        if (!label.isBlank()) {
            out.add(label);
        }
        out.add(value.length() >= columns ? value : " ".repeat(columns - value.length()) + value);
    }

    private String centre(String value) {
        if (value.length() >= columns) {
            return value.substring(0, columns);
        }
        int pad = (columns - value.length()) / 2;
        return " ".repeat(pad) + value;
    }

    private String separator() {
        return "-".repeat(columns);
    }

    /** Word wrap at the column width; a word longer than a line is broken rather than clipped. */
    private List<String> wrap(String value) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.strip().split("\\s+")) {
            while (word.length() > columns) {
                if (current.length() > 0) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                out.add(word.substring(0, columns));
                word = word.substring(columns);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= columns) {
                current.append(' ').append(word);
            } else {
                out.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out;
    }

    private List<String> wrapCentred(String value) {
        return wrap(value).stream().map(this::centre).toList();
    }
}
