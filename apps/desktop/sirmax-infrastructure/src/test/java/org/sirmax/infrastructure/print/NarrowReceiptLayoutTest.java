// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.print;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.shared.Money;

/**
 * The narrow receipt is the layout most likely to break silently: it has to stay legible on a 58 mm
 * roll, and a column that overflows just wraps into unreadable mush on real paper. Testing the text
 * directly — rather than parsing a PDF — is what makes that checkable.
 */
class NarrowReceiptLayoutTest {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Santo_Domingo"));

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    private static DocumentSnapshot receipt() {
        Money zero = Money.zero("DOP");
        return new DocumentSnapshot(
                DocumentKind.RECEIPT,
                "DOC-2026-000001",
                Instant.parse("2026-05-04T14:30:00Z"),
                new DocumentSnapshot.Institution(
                        "Ayuntamiento del Municipio de Santiago de los Caballeros",
                        Optional.empty(),
                        Optional.of("Santiago"),
                        Optional.of("401-00000-0"),
                        Optional.of("Calle del Sol esq. Benito Monción"),
                        Optional.of("809-582-1000"),
                        Optional.empty(),
                        Optional.of("santiago.gob.do"),
                        Optional.empty()),
                new DocumentSnapshot.Customer(
                        "Ana Rodríguez Cruz",
                        Optional.of("Cédula"),
                        Optional.of("031-0123456-7"),
                        Optional.empty(),
                        Optional.empty()),
                List.of(
                        new DocumentSnapshot.Line(
                                "Certificación de uso de suelo para local comercial",
                                Optional.empty(),
                                2,
                                Optional.empty(),
                                dop("250.00"),
                                zero,
                                zero,
                                dop("500.00"))),
                new DocumentSnapshot.Totals(
                        dop("500.00"), zero, zero, dop("500.00"), dop("500.00"), zero),
                Optional.of(
                        new DocumentSnapshot.PaymentInfo(
                                "Efectivo",
                                dop("500.00"),
                                Optional.of(dop("1000.00")),
                                Optional.of(dop("500.00")),
                                Optional.empty(),
                                Instant.parse("2026-05-04T14:30:00Z"),
                                Optional.of("Cajera"))),
                Optional.of("TRM-2026-000012"),
                Optional.of("Cajera"),
                Optional.of("Gracias por su pago"),
                "34AC-6799-QQTU");
    }

    private List<String> lay(PaperFormat format) {
        return new NarrowReceiptLayout(format.monospaceColumns(), STAMP).lay(receipt(), false);
    }

    @Test
    void nothingOverflowsTheRollWidth() {
        for (PaperFormat format : List.of(PaperFormat.NARROW_58, PaperFormat.NARROW_80)) {
            assertThat(lay(format))
                    .as("lines within %d columns", format.monospaceColumns())
                    .allSatisfy(line -> assertThat(line.length()).isLessThanOrEqualTo(format.monospaceColumns()));
        }
    }

    @Test
    void aLongConceptWrapsRatherThanTruncating() {
        String joined = String.join("\n", lay(PaperFormat.NARROW_58));

        // the whole concept must survive, split across lines
        assertThat(joined.replace("\n", " ").replaceAll(" +", " "))
                .contains("Certificación de uso de suelo para local comercial");
        assertThat(joined).doesNotContain("…");
    }

    @Test
    void everyFigureTheCitizenNeedsIsOnTheReceipt() {
        String joined = String.join("\n", lay(PaperFormat.NARROW_80));

        assertThat(joined)
                .contains("DOC-2026-000001")
                .contains("Ana Rodríguez Cruz")
                .contains("031-0123456-7")
                .contains("TOTAL")
                .contains("Pagado")
                .contains("CAMBIO")
                .contains("34AC-6799-QQTU");
    }

    @Test
    void aReprintSaysSoAtTheTop() {
        List<String> lines =
                new NarrowReceiptLayout(PaperFormat.NARROW_80.monospaceColumns(), STAMP)
                        .lay(receipt(), true);

        assertThat(lines.get(0)).contains("COPIA");
    }

    @Test
    void amountsAreRightAlignedInTheirColumn() {
        List<String> lines = lay(PaperFormat.NARROW_80);
        int columns = PaperFormat.NARROW_80.monospaceColumns();

        List<String> totals = lines.stream().filter(l -> l.startsWith("TOTAL")).toList();
        assertThat(totals).hasSize(1);
        assertThat(totals.get(0)).hasSize(columns);
        assertThat(totals.get(0).stripTrailing()).isEqualTo(totals.get(0));
    }
}
