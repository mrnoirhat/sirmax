// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.print;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.sirmax.application.port.DocumentRenderer;
import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.shared.Money;
import org.sirmax.shared.SirmaxException;

/**
 * Renders a {@link DocumentSnapshot} to a real PDF with PDFBox (master prompt §59B, §59E).
 *
 * <p>Two layouts, because the two printers are genuinely different machines:
 *
 * <ul>
 *   <li><b>Letter</b> (§59B.2) — letterhead, invoice identity, customer block, a ruled detail table,
 *       totals, payment block, footer with the verification code and QR.
 *   <li><b>Narrow</b> (§59B.1) — one monospaced column on continuous roll paper, no rules, no
 *       colour, no logo. Impact printers at 180 dpi turn hairlines into smudges and tint into grey
 *       mush, so the layout leans entirely on text and whitespace.
 * </ul>
 *
 * <p>The renderer touches nothing but the snapshot. That is the guarantee behind §59F: there is no
 * code path by which today's branding could reach a 2026 reprint.
 */
public final class PdfDocumentRenderer implements DocumentRenderer {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private static final float LETTER_MARGIN = 54f; // 0.75 in
    private static final float NARROW_MARGIN = 10f;

    /** Where the detail columns start, as fractions of the printable width. */
    private static final float COL_QUANTITY = 0.52f;
    private static final float COL_UNIT_PRICE = 0.62f;
    private static final float COL_DISCOUNT = 0.76f;
    private static final float COL_AMOUNT = 0.88f;

    private final QrCodes qrCodes = new QrCodes();

    @Override
    public byte[] render(DocumentSnapshot snapshot, PaperFormat format, boolean markAsCopy) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (format.isNarrow()) {
                renderNarrow(document, snapshot, format, markAsCopy);
            } else {
                renderLetter(document, snapshot, format, markAsCopy);
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new SirmaxException(
                    "Could not render document " + snapshot.documentNumber(), e);
        }
    }

    // ── §59B.2 — US Letter office invoice ────────────────────────────────

    private void renderLetter(
            PDDocument document, DocumentSnapshot s, PaperFormat format, boolean markAsCopy)
            throws IOException {
        PDPage page = new PDPage(new PDRectangle(format.widthPoints(), format.heightPoints()));
        document.addPage(page);

        float width = format.widthPoints();
        float printable = width - 2 * LETTER_MARGIN;

        try (PDPageContentStream c = new PDPageContentStream(document, page)) {
            Cursor cursor = new Cursor(format.heightPoints() - LETTER_MARGIN);

            if (markAsCopy) {
                text(c, bold(), 9, LETTER_MARGIN, cursor.y, "— COPIA / REIMPRESIÓN —");
                cursor.down(16);
            }

            // ── letterhead ──
            DocumentSnapshot.Institution institution = s.institution();
            text(c, bold(), 15, LETTER_MARGIN, cursor.y, institution.name());
            cursor.down(15);
            for (String line : institutionLines(institution)) {
                text(c, regular(), 8.5f, LETTER_MARGIN, cursor.y, line);
                cursor.down(11);
            }
            cursor.down(6);
            rule(c, LETTER_MARGIN, cursor.y, width - LETTER_MARGIN);
            cursor.down(20);

            // ── invoice identity ──
            text(c, bold(), 13, LETTER_MARGIN, cursor.y, titleFor(s));
            String number = s.documentNumber();
            textRight(c, bold(), 13, width - LETTER_MARGIN, cursor.y, number);
            cursor.down(14);
            text(c, regular(), 9, LETTER_MARGIN, cursor.y, STAMP.format(s.issuedAt()));
            if (s.reference().isPresent()) {
                textRight(
                        c, regular(), 9, width - LETTER_MARGIN, cursor.y,
                        "Ref. " + s.reference().get());
            }
            cursor.down(20);

            // ── customer block ──
            text(c, bold(), 9, LETTER_MARGIN, cursor.y, "CIUDADANO");
            cursor.down(12);
            text(c, regular(), 10, LETTER_MARGIN, cursor.y, s.customer().name());
            cursor.down(12);
            String identification =
                    s.customer()
                            .identificationNumber()
                            .map(n -> s.customer().identificationType().orElse("Cédula") + ": " + n)
                            .orElse("");
            if (!identification.isEmpty()) {
                text(c, regular(), 9, LETTER_MARGIN, cursor.y, identification);
                cursor.down(12);
            }
            if (s.customer().address().isPresent()) {
                text(c, regular(), 9, LETTER_MARGIN, cursor.y, s.customer().address().get());
                cursor.down(12);
            }
            cursor.down(10);

            // ── detail table ──
            rule(c, LETTER_MARGIN, cursor.y + 10, width - LETTER_MARGIN);
            text(c, bold(), 8.5f, LETTER_MARGIN, cursor.y, "CONCEPTO");
            textRight(c, bold(), 8.5f, col(printable, COL_QUANTITY), cursor.y, "CANT.");
            textRight(c, bold(), 8.5f, col(printable, COL_UNIT_PRICE), cursor.y, "PRECIO");
            textRight(c, bold(), 8.5f, col(printable, COL_DISCOUNT), cursor.y, "DESC.");
            textRight(c, bold(), 8.5f, col(printable, COL_AMOUNT), cursor.y, "IMPORTE");
            cursor.down(6);
            rule(c, LETTER_MARGIN, cursor.y + 4, width - LETTER_MARGIN);
            cursor.down(12);

            for (DocumentSnapshot.Line line : s.lines()) {
                text(c, regular(), 9, LETTER_MARGIN, cursor.y, truncate(line.concept(), 44));
                textRight(
                        c,
                        regular(),
                        9,
                        col(printable, COL_QUANTITY),
                        cursor.y,
                        String.valueOf(line.quantity()));
                textRight(
                        c, regular(), 9, col(printable, COL_UNIT_PRICE), cursor.y,
                        amount(line.unitPrice()));
                textRight(
                        c, regular(), 9, col(printable, COL_DISCOUNT), cursor.y,
                        line.discount().isZero() ? "—" : amount(line.discount()));
                textRight(
                        c, regular(), 9, col(printable, COL_AMOUNT), cursor.y,
                        amount(line.lineTotal()));
                cursor.down(13);
            }

            cursor.down(4);
            rule(c, col(printable, 0.55f), cursor.y + 8, width - LETTER_MARGIN);
            cursor.down(8);

            // ── totals ──
            DocumentSnapshot.Totals totals = s.totals();
            totalRow(c, printable, cursor, "Subtotal", totals.subtotal(), false);
            if (!totals.discount().isZero()) {
                totalRow(c, printable, cursor, "Descuento", totals.discount(), false);
            }
            if (!totals.surcharge().isZero()) {
                totalRow(c, printable, cursor, "Cargos", totals.surcharge(), false);
            }
            totalRow(c, printable, cursor, "TOTAL", totals.total(), true);
            totalRow(c, printable, cursor, "Pagado", totals.paid(), false);
            totalRow(c, printable, cursor, "Pendiente", totals.balance(), false);
            cursor.down(14);

            // ── payment block ──
            if (s.payment().isPresent()) {
                DocumentSnapshot.PaymentInfo payment = s.payment().get();
                text(c, bold(), 9, LETTER_MARGIN, cursor.y, "PAGO");
                cursor.down(12);
                text(
                        c,
                        regular(),
                        9,
                        LETTER_MARGIN,
                        cursor.y,
                        payment.method()
                                + "  ·  "
                                + amount(payment.amount())
                                + payment.reference().map(r -> "  ·  Ref. " + r).orElse("")
                                + "  ·  "
                                + STAMP.format(payment.paidAt()));
                cursor.down(12);
                if (payment.change().filter(Money::isPositive).isPresent()) {
                    text(
                            c, regular(), 9, LETTER_MARGIN, cursor.y,
                            "Cambio: " + amount(payment.change().get()));
                    cursor.down(12);
                }
                cursor.down(6);
            }

            // ── footer: verification, QR, notes ──
            float footerY = LETTER_MARGIN + 78;
            rule(c, LETTER_MARGIN, footerY + 66, width - LETTER_MARGIN);
            text(c, bold(), 8.5f, LETTER_MARGIN, footerY + 52, "Código de verificación");
            text(c, mono(), 11, LETTER_MARGIN, footerY + 38, s.verificationCode());
            if (s.issuedByName().isPresent()) {
                text(
                        c, regular(), 8, LETTER_MARGIN, footerY + 24,
                        "Atendido por: " + s.issuedByName().get());
            }
            if (s.footerNote().isPresent()) {
                text(
                        c, regular(), 7.5f, LETTER_MARGIN, footerY + 10,
                        truncate(s.footerNote().get(), 110));
            }

            drawQr(document, c, s, width - LETTER_MARGIN - 62, footerY + 4, 62f);
        }
    }

    // ── §59B.1 — narrow counter receipt ─────────────────────────────────

    private void renderNarrow(
            PDDocument document, DocumentSnapshot s, PaperFormat format, boolean markAsCopy)
            throws IOException {
        int columns = format.monospaceColumns();
        List<String> lines = new NarrowReceiptLayout(columns, STAMP).lay(s, markAsCopy);

        // Roll paper is continuous, so the page grows to fit rather than paginating.
        float lineHeight = 9.5f;
        float height = NARROW_MARGIN * 2 + lineHeight * lines.size() + 70f;
        PDPage page = new PDPage(new PDRectangle(format.widthPoints(), height));
        document.addPage(page);

        try (PDPageContentStream c = new PDPageContentStream(document, page)) {
            float y = height - NARROW_MARGIN - lineHeight;
            for (String line : lines) {
                // Monospaced throughout: the column alignment the layout computed only holds if
                // every glyph is the same width.
                text(c, mono(), 7.5f, NARROW_MARGIN, y, line);
                y -= lineHeight;
            }
            drawQr(document, c, s, (format.widthPoints() - 52f) / 2f, y - 56f, 52f);
        }
    }

    private void drawQr(
            PDDocument document, PDPageContentStream c, DocumentSnapshot s, float x, float y, float size)
            throws IOException {
        var image = qrCodes.encode(s.verificationCode(), (int) Math.ceil(size * 3));
        if (image.isEmpty()) {
            return;
        }
        PDImageXObject qr = LosslessFactory.createFromImage(document, image.get());
        c.drawImage(qr, x, y, size, size);
    }

    // ── drawing helpers ─────────────────────────────────────────────────

    /** Mutable Y position, so the layout reads top-to-bottom like the page does. */
    private static final class Cursor {
        private float y;

        Cursor(float y) {
            this.y = y;
        }

        void down(float points) {
            y -= points;
        }
    }

    private void totalRow(
            PDPageContentStream c, float printable, Cursor cursor, String label, Money value, boolean strong)
            throws IOException {
        var font = strong ? bold() : regular();
        float size = strong ? 11 : 9;
        textRight(c, font, size, col(printable, COL_DISCOUNT), cursor.y, label);
        textRight(c, font, size, col(printable, COL_AMOUNT), cursor.y, amount(value));
        cursor.down(strong ? 16 : 13);
    }

    private static float col(float printable, float fraction) {
        return LETTER_MARGIN + printable * fraction;
    }

    private static List<String> institutionLines(DocumentSnapshot.Institution i) {
        var lines = new java.util.ArrayList<String>();
        i.department().ifPresent(lines::add);
        i.address().ifPresent(lines::add);
        String contact =
                java.util.stream.Stream.of(i.phone(), i.email(), i.website())
                        .flatMap(java.util.Optional::stream)
                        .reduce((a, b) -> a + "  ·  " + b)
                        .orElse("");
        if (!contact.isBlank()) {
            lines.add(contact);
        }
        i.legalIdentifier().ifPresent(id -> lines.add("RNC: " + id));
        return lines;
    }

    private static String titleFor(DocumentSnapshot s) {
        return switch (s.kind()) {
            case INVOICE -> "FACTURA";
            case RECEIPT -> "RECIBO";
            case CERTIFICATE -> "CERTIFICACIÓN";
            case OFFICIAL_LETTER -> "CARTA OFICIAL";
            case PERMIT -> "PERMISO";
            case REGISTRY_COPY -> "COPIA CERTIFICADA";
            case OTHER -> "DOCUMENTO";
        };
    }

    static String amount(Money money) {
        return String.format(
                Locale.US,
                "%s %,.2f",
                money.currency().getCurrencyCode(),
                money.toDecimal());
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static void text(
            PDPageContentStream c, PDType1Font font, float size, float x, float y, String value)
            throws IOException {
        c.beginText();
        c.setFont(font, size);
        c.newLineAtOffset(x, y);
        c.showText(sanitize(value));
        c.endText();
    }

    private static void textRight(
            PDPageContentStream c, PDType1Font font, float size, float rightX, float y, String value)
            throws IOException {
        String safe = sanitize(value);
        float width = font.getStringWidth(safe) / 1000 * size;
        text(c, font, size, rightX - width, y, safe);
    }

    private static void rule(PDPageContentStream c, float fromX, float y, float toX)
            throws IOException {
        c.setLineWidth(0.6f);
        c.moveTo(fromX, y);
        c.lineTo(toX, y);
        c.stroke();
    }

    /**
     * The Standard 14 fonts are WinAnsi-encoded, which covers Spanish but not every character an
     * operator might paste in. Anything outside it becomes '?' rather than throwing mid-page and
     * leaving the citizen without a receipt.
     */
    private static String sanitize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            out.append(ch < 256 || "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ".indexOf(ch) >= 0 ? ch : '?');
        }
        return out.toString();
    }

    private static PDType1Font regular() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private static PDType1Font bold() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    private static PDType1Font mono() {
        return new PDType1Font(Standard14Fonts.FontName.COURIER);
    }
}
