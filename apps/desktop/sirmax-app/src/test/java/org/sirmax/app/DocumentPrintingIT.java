// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.port.DocumentPrinter;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.IssueDocument;
import org.sirmax.application.usecase.IssueInvoice;
import org.sirmax.application.usecase.ManageCashSession;
import org.sirmax.application.usecase.PrintDocument;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Documents, PDFs and printing against the real graph — master prompt §59D–§59F.
 *
 * <p>The printer is a recording double, because a CI runner has no printers. Everything else is
 * real: real PDFBox rendering, real snapshot persistence, real audit trail. The §59F guarantee gets
 * its own test, since it is the one this whole phase exists to keep.
 */
class DocumentPrintingIT {

    /** Records what was sent to print instead of printing it. */
    private static final class RecordingPrinter implements DocumentPrinter {
        final List<byte[]> jobs = new ArrayList<>();
        final List<PrinterProfile> profiles = new ArrayList<>();
        boolean operatorCancels;

        @Override
        public List<String> availablePrinters() {
            return List.of("SIRMAX Test Printer");
        }

        @Override
        public Optional<String> defaultPrinter() {
            return Optional.of("SIRMAX Test Printer");
        }

        @Override
        public boolean print(byte[] pdf, PrinterProfile profile) {
            if (operatorCancels) {
                return false;
            }
            jobs.add(pdf);
            profiles.add(profile);
            return true;
        }
    }

    private SqliteDatabase database;
    private CompositionRoot root;
    private RecordingPrinter printer;
    private PrintDocument printDocument;
    private Session cashier;

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        root = CompositionRoot.bootstrap(database);
        printer = new RecordingPrinter();
        // The one seam: everything else in this test is the production graph.
        printDocument =
                new PrintDocument(
                        root.documents(),
                        new org.sirmax.infrastructure.print.PdfDocumentRenderer(),
                        printer,
                        root.ids(),
                        root.clock(),
                        new org.sirmax.infrastructure.persistence.JdbcUnitOfWork(database),
                        root.auditFor());

        root.provisionInitialAdmin()
                .execute(
                        new ProvisionInitialAdmin.Command(
                                "Ayuntamiento de Santiago",
                                "Santiago",
                                "DO",
                                "admin",
                                "Administradora",
                                "una-contrasena-larga".toCharArray()));
        cashier =
                root.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow();

        seedBranding();
        seedService();
        seedPrinterProfiles();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private void seedBranding() {
        var unit = root.organizationRepository().findActive().orElseThrow();
        var overrides = new InstitutionProfile.Overrides();
        overrides.legalIdentifier = "401-00000-0";
        overrides.address = "Calle del Sol esq. Benito Monción";
        overrides.phone = "809-582-1000";
        overrides.website = "santiago.gob.do";
        overrides.invoiceFooter = "Gracias por su pago";
        root.organizationRepository()
                .saveProfile(InstitutionProfile.empty(unit.id()).with(overrides));
    }

    private void seedService() {
        var now = root.clock().now();
        var catalog = root.serviceCatalogRepository();
        catalog.saveCategory(ServiceCategory.create("cat-1", "URB", "Planeamiento", 1, now));
        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1", "URB-USO", "cat-1", "Certificación de uso de suelo",
                        ServiceType.CON_TASA, "DO", now);
        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, now);
        version.setRequiresPayment(true);
        version.setFeeRules(
                List.of(
                        FeeRule.fixed(
                                "fee-1",
                                ChargeType.TASA,
                                "Certificación de uso de suelo",
                                "DOP",
                                50_000L,
                                LocalDate.of(2026, 1, 1))));
        version.publish(now);
        definition.setCurrentVersion("ver-1", now);
        catalog.saveDefinition(definition);
        catalog.saveVersion(version);
    }

    private void seedPrinterProfiles() {
        var now = root.clock().now();
        root.documents()
                .save(
                        new PrinterProfile(
                                "pp-letter",
                                "Oficina",
                                Optional.of("SIRMAX Test Printer"),
                                PaperFormat.LETTER,
                                Optional.empty(),
                                true,
                                1,
                                false,
                                now,
                                now));
        root.documents()
                .save(
                        new PrinterProfile(
                                "pp-narrow",
                                "Caja 1",
                                Optional.of("SIRMAX Test Printer"),
                                PaperFormat.NARROW_80,
                                Optional.empty(),
                                true,
                                1,
                                true,
                                now,
                                now));
    }

    private Invoice paidInvoice() {
        Person citizen =
                root.registerPerson()
                        .execute(
                                new RegisterPerson.Command(
                                        cashier,
                                        "Ana",
                                        "Rodríguez Cruz",
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        Procedure procedure =
                root.startProcedure()
                        .execute(
                                new StartProcedure.Command(
                                        cashier,
                                        "svc-1",
                                        PartyRef.person(citizen.id()),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        root.manageCashSession()
                .open(
                        new ManageCashSession.OpenCommand(
                                cashier, dop("2000.00"), Optional.empty(), "test"))
                .orElseThrow();
        Invoice invoice =
                root.issueInvoice()
                        .execute(
                                new IssueInvoice.Command(
                                        cashier,
                                        procedure.id(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        root.registerPayment()
                .execute(
                        new RegisterPayment.Command(
                                cashier,
                                invoice.id(),
                                PaymentMethod.CASH,
                                dop("1000.00"),
                                Optional.of(dop("1000.00")),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
        return root.billing().findInvoiceById(invoice.id()).orElseThrow();
    }

    private IssuedDocument issue(Invoice invoice, DocumentKind kind, PaperFormat format) {
        return root.issueDocument()
                .execute(
                        new IssueDocument.Command(
                                cashier, invoice.id(), kind, format, Optional.empty(), "test"))
                .orElseThrow();
    }

    private Result<PrintDocument.Outcome> print(IssuedDocument document) {
        return printDocument.execute(
                new PrintDocument.Command(
                        cashier,
                        document.id(),
                        Optional.empty(),
                        Optional.empty(),
                        "TEST-WORKSTATION",
                        "test"));
    }

    @Test
    void bothMandatoryFormatsProduceRealPdfs() {
        Invoice invoice = paidInvoice();

        for (PaperFormat format : List.of(PaperFormat.LETTER, PaperFormat.NARROW_80)) {
            DocumentKind kind =
                    format.isNarrow() ? DocumentKind.RECEIPT : DocumentKind.INVOICE;
            IssuedDocument document = issue(invoice, kind, format);

            byte[] pdf = printDocument.renderOnly(cashier, document.id()).orElseThrow();

            // A real PDF, not a screenshot (§59E)
            assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1))
                    .isEqualTo("%PDF-");
            assertThat(pdf.length).isGreaterThan(1000);
        }
    }

    @Test
    void anIssuedDocumentCarriesItsOwnFrozenBranding() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER);

        var snapshot = document.snapshot();
        assertThat(snapshot.institution().name()).isEqualTo("Ayuntamiento de Santiago");
        assertThat(snapshot.institution().legalIdentifier()).contains("401-00000-0");
        assertThat(snapshot.customer().name()).isEqualTo("Ana Rodríguez Cruz");
        assertThat(snapshot.totals().total()).isEqualTo(dop("500.00"));
        assertThat(snapshot.payment().orElseThrow().change()).contains(dop("500.00"));
        assertThat(snapshot.footerNote()).contains("Gracias por su pago");
    }

    /** The §59F guarantee: this is the reason the whole snapshot mechanism exists. */
    @Test
    void aLaterRebrandDoesNotRewriteAnAlreadyIssuedDocument() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER);

        // The municipality rebrands: new RNC, new address, new footer.
        var unit = root.organizationRepository().findActive().orElseThrow();
        var overrides = new InstitutionProfile.Overrides();
        overrides.legalIdentifier = "999-99999-9";
        overrides.address = "Otra dirección completamente distinta";
        overrides.invoiceFooter = "Nuevo mensaje institucional";
        root.organizationRepository()
                .saveProfile(InstitutionProfile.empty(unit.id()).with(overrides));

        IssuedDocument reloaded = root.documents().findById(document.id()).orElseThrow();

        assertThat(reloaded.snapshot().institution().legalIdentifier()).contains("401-00000-0");
        assertThat(reloaded.snapshot().institution().address())
                .contains("Calle del Sol esq. Benito Monción");
        assertThat(reloaded.snapshot().footerNote()).contains("Gracias por su pago");

        // and a new document does pick up the new branding
        IssuedDocument fresh = issue(invoice, DocumentKind.RECEIPT, PaperFormat.NARROW_80);
        assertThat(fresh.snapshot().institution().legalIdentifier()).contains("999-99999-9");
    }

    @Test
    void reprintingNeverRenumbersAndIsMarkedAsACopy() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER);
        String number = document.documentNumber();

        assertThat(print(document).orElseThrow().wasReprint()).isFalse();

        IssuedDocument afterFirst = root.documents().findById(document.id()).orElseThrow();
        assertThat(afterFirst.printCount()).isEqualTo(1);
        assertThat(afterFirst.isReprintNext()).isTrue();

        PrintDocument.Outcome second = print(afterFirst).orElseThrow();
        assertThat(second.wasReprint()).isTrue();
        assertThat(second.document().documentNumber()).isEqualTo(number);
        assertThat(second.document().printCount()).isEqualTo(2);

        // and the invoice behind it is untouched — no duplicated payment (§59D)
        Invoice reloaded = root.billing().findInvoiceById(invoice.id()).orElseThrow();
        assertThat(reloaded.paid()).isEqualTo(dop("500.00"));
        assertThat(root.billing().findPaymentsByInvoice(invoice.id())).hasSize(1);
    }

    @Test
    void everyPrintLandsInTheHistoryAndTheAuditTrail() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.RECEIPT, PaperFormat.NARROW_80);

        print(document);
        print(root.documents().findById(document.id()).orElseThrow());

        var history = root.documents().printHistory(document.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).isReprint()).isFalse();
        assertThat(history.get(1).isReprint()).isTrue();

        List<String> actions =
                root.auditTrail()
                        .search(
                                Optional.of("IssuedDocument"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                50,
                                0)
                        .stream()
                        .map(org.sirmax.domain.audit.AuditEvent::action)
                        .toList();
        assertThat(actions).contains("document.issued", "document.printed", "document.reprinted");
    }

    @Test
    void aCancelledPrintDialogRecordsNothing() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER);
        printer.operatorCancels = true;

        PrintDocument.Outcome outcome = print(document).orElseThrow();

        assertThat(outcome.sentToPrinter()).isFalse();
        assertThat(root.documents().findById(document.id()).orElseThrow().printCount()).isZero();
        assertThat(root.documents().printHistory(document.id())).isEmpty();
    }

    @Test
    void theCounterProfileIsSilentAndTheOfficeOneIsNot() {
        Invoice invoice = paidInvoice();

        print(issue(invoice, DocumentKind.RECEIPT, PaperFormat.NARROW_80));
        assertThat(printer.profiles.get(0).silent()).isTrue();

        print(issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER));
        assertThat(printer.profiles.get(1).silent()).isFalse();
    }

    @Test
    void aDocumentCanBeFoundByTheCodePrintedOnIt() {
        Invoice invoice = paidInvoice();
        IssuedDocument document = issue(invoice, DocumentKind.INVOICE, PaperFormat.LETTER);

        assertThat(
                        root.documents()
                                .findByVerificationCode(document.verificationCode())
                                .map(IssuedDocument::documentNumber))
                .contains(document.documentNumber());
    }

    @Test
    void aReceiptCannotBeIssuedBeforeAnyMoneyArrives() {
        Person citizen =
                root.registerPerson()
                        .execute(
                                new RegisterPerson.Command(
                                        cashier,
                                        "Pedro",
                                        "Martínez",
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        Procedure procedure =
                root.startProcedure()
                        .execute(
                                new StartProcedure.Command(
                                        cashier,
                                        "svc-1",
                                        PartyRef.person(citizen.id()),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        Invoice unpaid =
                root.issueInvoice()
                        .execute(
                                new IssueInvoice.Command(
                                        cashier,
                                        procedure.id(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();

        Result<?> result =
                root.issueDocument()
                        .execute(
                                new IssueDocument.Command(
                                        cashier,
                                        unpaid.id(),
                                        DocumentKind.RECEIPT,
                                        PaperFormat.NARROW_80,
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("document.nothing_paid");
    }
}
