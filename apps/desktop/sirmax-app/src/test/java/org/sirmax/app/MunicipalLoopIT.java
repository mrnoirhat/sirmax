// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.AdvanceProcedure;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.IssueInvoice;
import org.sirmax.application.usecase.ManageCashSession;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RefundPayment;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.UpdateProcedureRequirement;
import org.sirmax.application.usecase.VoidInvoice;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * The whole municipal loop against the real graph — master prompt §10, scenario A:
 *
 * <pre>{@code
 * Ciudadano → Servicio → Trámite → Requisitos → Tasa → Factura → Pago → Auditoría
 * }</pre>
 *
 * <p>Real SQLite, real migrations, real numbering, real money. Nothing is stubbed, so a mistake
 * anywhere from the schema to the permission checks fails here rather than in production.
 */
class MunicipalLoopIT {

    private SqliteDatabase database;
    private CompositionRoot root;
    private Session cashier;

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        root = CompositionRoot.bootstrap(database);

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
        seedPaidCertificate();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    /** "Certificación de uso de suelo": one requirement, RD$500, a payment checkpoint in the flow. */
    private void seedPaidCertificate() {
        var catalog = root.serviceCatalogRepository();
        var now = root.clock().now();

        catalog.saveCategory(ServiceCategory.create("cat-1", "URB", "Planeamiento", 1, now));
        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1",
                        "URB-USO",
                        "cat-1",
                        "Certificación de uso de suelo",
                        ServiceType.CON_TASA,
                        "DO",
                        now);
        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, now);
        version.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument(
                                "cedula", "Cédula de identidad", RequirementStage.INTAKE)));
        version.setRequiresPayment(true);
        version.setFeeRules(
                List.of(
                        FeeRule.fixed(
                                "fee-1",
                                ChargeType.TASA,
                                "Certificación de uso de suelo",
                                "DOP",
                                50_000L, // RD$500.00 in minor units
                                LocalDate.of(2026, 1, 1))));
        version.setWorkflow(
                new WorkflowDefinition(
                        "recepcion",
                        List.of(
                                WorkflowStep.task("recepcion", "Recepción", "cobro"),
                                new WorkflowStep(
                                        "cobro",
                                        "Cobro",
                                        StepType.PAYMENT_CHECKPOINT,
                                        Optional.empty(),
                                        1,
                                        List.of(Transition.advance("emision"))),
                                new WorkflowStep(
                                        "emision",
                                        "Emisión",
                                        StepType.DOCUMENT_OUTPUT,
                                        Optional.empty(),
                                        1,
                                        List.of(Transition.terminal(TransitionKind.APPROVE))))));
        version.publish(now);
        definition.setCurrentVersion("ver-1", now);

        catalog.saveDefinition(definition);
        catalog.saveVersion(version);
    }

    private Person registerCitizen() {
        return root.registerPerson()
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
    }

    private Procedure openCaseFor(Person citizen) {
        return root.startProcedure()
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
    }

    private void satisfyCedula(Procedure procedure) {
        root.updateProcedureRequirement()
                .execute(
                        new UpdateProcedureRequirement.Command(
                                cashier,
                                procedure.id(),
                                "cedula",
                                UpdateProcedureRequirement.Action.SATISFY,
                                Optional.empty(),
                                "test"));
    }

    private Invoice issueFor(Procedure procedure) {
        return root.issueInvoice()
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
    }

    private void openDrawer() {
        root.manageCashSession()
                .open(
                        new ManageCashSession.OpenCommand(
                                cashier, dop("2000.00"), Optional.empty(), "test"))
                .orElseThrow();
    }

    @Test
    void theWholeLoopRunsFromCitizenToPaidAndApproved() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();

        Invoice invoice = issueFor(procedure);
        assertThat(invoice.number()).contains("FACT-2026-000001");
        assertThat(invoice.total()).isEqualTo(dop("500.00"));
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);

        // The case is parked on money, and the payment checkpoint refuses to advance.
        assertThat(root.procedureRepository().findById(procedure.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.WAITING_PAYMENT);

        // Moving *to* the checkpoint is fine; it is leaving it that needs the money.
        advance(procedure);
        Procedure atCheckpoint = root.procedureRepository().findById(procedure.id()).orElseThrow();
        assertThat(atCheckpoint.currentStepKey()).contains("cobro");
        assertThat(atCheckpoint.status()).isEqualTo(ProcedureStatus.WAITING_PAYMENT);
        Result<?> blocked =
                root.advanceProcedure()
                        .execute(
                                new AdvanceProcedure.Command(
                                        cashier,
                                        procedure.id(),
                                        TransitionKind.ADVANCE,
                                        Optional.empty(),
                                        "test"));
        assertThat(((Result.Err<?>) blocked).messageKey()).isEqualTo("procedure.payment_required");

        RegisterPayment.Receipt receipt =
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

        assertThat(receipt.payment().code()).isEqualTo("REC-2026-000001");
        assertThat(receipt.change()).isEqualTo(dop("500.00"));
        assertThat(receipt.isSettled()).isTrue();
        assertThat(receipt.invoice().status()).isEqualTo(InvoiceStatus.PAID);

        // Paid: the checkpoint opens and the case can be approved.
        advance(procedure);
        assertThat(root.procedureRepository().findById(procedure.id()).orElseThrow().currentStepKey())
                .contains("emision");
        root.advanceProcedure()
                .execute(
                        new AdvanceProcedure.Command(
                                cashier, procedure.id(), TransitionKind.APPROVE, Optional.empty(), "test"));
        assertThat(root.procedureRepository().findById(procedure.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.APPROVED);
    }

    @Test
    void aPartialPaymentLeavesTheCaseWaitingUntilTheBalanceClears() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();
        Invoice invoice = issueFor(procedure);

        RegisterPayment.Receipt first = pay(invoice, dop("200.00"));
        assertThat(first.invoice().status()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(first.invoice().balance()).isEqualTo(dop("300.00"));
        assertThat(root.procedureRepository().findById(procedure.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.WAITING_PAYMENT);

        RegisterPayment.Receipt second = pay(invoice, dop("300.00"));
        assertThat(second.isSettled()).isTrue();
        assertThat(root.procedureRepository().findById(procedure.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.IN_PROGRESS);
    }

    @Test
    void cashCannotBeTakenWithoutAnOpenDrawerButATransferCan() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        Invoice invoice = issueFor(procedure);

        Result<?> withoutDrawer =
                root.registerPayment()
                        .execute(
                                new RegisterPayment.Command(
                                        cashier,
                                        invoice.id(),
                                        PaymentMethod.CASH,
                                        dop("500.00"),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"));
        assertThat(((Result.Err<?>) withoutDrawer).messageKey()).isEqualTo("cash.no_open_session");

        Result<?> transferWithoutReference =
                root.registerPayment()
                        .execute(
                                new RegisterPayment.Command(
                                        cashier,
                                        invoice.id(),
                                        PaymentMethod.BANK_TRANSFER,
                                        dop("500.00"),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"));
        assertThat(((Result.Err<?>) transferWithoutReference).messageKey())
                .isEqualTo("payment.reference_required");

        RegisterPayment.Receipt ok =
                root.registerPayment()
                        .execute(
                                new RegisterPayment.Command(
                                        cashier,
                                        invoice.id(),
                                        PaymentMethod.BANK_TRANSFER,
                                        dop("500.00"),
                                        Optional.empty(),
                                        Optional.of("TR-99811"),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(ok.isSettled()).isTrue();
    }

    @Test
    void aPaidInvoiceMustBeRefundedBeforeItCanBeVoided() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();
        Invoice invoice = issueFor(procedure);
        RegisterPayment.Receipt receipt = pay(invoice, dop("500.00"));

        Result<?> tooEarly =
                root.voidInvoice()
                        .execute(
                                new VoidInvoice.Command(
                                        cashier, invoice.id(), "Emitida por error", "test"));
        assertThat(((Result.Err<?>) tooEarly).messageKey()).isEqualTo("invoice.refund_before_void");

        root.refundPayment()
                .execute(
                        new RefundPayment.Command(
                                cashier,
                                receipt.payment().id(),
                                Optional.empty(),
                                "Cobro duplicado",
                                "test"))
                .orElseThrow();

        Invoice voided =
                root.voidInvoice()
                        .execute(
                                new VoidInvoice.Command(
                                        cashier, invoice.id(), "Emitida por error", "test"))
                        .orElseThrow();
        assertThat(voided.status()).isEqualTo(InvoiceStatus.VOIDED);
        assertThat(voided.voidReason()).contains("Emitida por error");
        // The spent number is never handed out again (§27).
        assertThat(voided.number()).contains("FACT-2026-000001");
    }

    @Test
    void refundingMoreThanWasCollectedIsRefused() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();
        Invoice invoice = issueFor(procedure);
        RegisterPayment.Receipt receipt = pay(invoice, dop("500.00"));

        Result<?> tooMuch =
                root.refundPayment()
                        .execute(
                                new RefundPayment.Command(
                                        cashier,
                                        receipt.payment().id(),
                                        Optional.of(dop("600.00")),
                                        "Error",
                                        "test"));
        assertThat(((Result.Err<?>) tooMuch).messageKey()).isEqualTo("refund.exceeds_payment");

        // A partial refund is fine, and the remainder stays refundable.
        root.refundPayment()
                .execute(
                        new RefundPayment.Command(
                                cashier,
                                receipt.payment().id(),
                                Optional.of(dop("200.00")),
                                "Ajuste",
                                "test"))
                .orElseThrow();
        assertThat(root.billing().findInvoiceById(invoice.id()).orElseThrow().paid())
                .isEqualTo(dop("300.00"));
    }

    @Test
    void theDrawerReconcilesAgainstWhatWasActuallyCounted() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();
        pay(issueFor(procedure), dop("500.00"));

        ManageCashSession.Closing closing =
                root.manageCashSession()
                        .close(
                                new ManageCashSession.CloseCommand(
                                        cashier, dop("2300.00"), Optional.empty(), "test"))
                        .orElseThrow();

        assertThat(closing.openingFloat()).isEqualTo(dop("2000.00"));
        assertThat(closing.cashCollected()).isEqualTo(dop("500.00"));
        assertThat(closing.expected()).isEqualTo(dop("2500.00"));
        assertThat(closing.difference()).isEqualTo(dop("-200.00"));
        assertThat(closing.balances()).isFalse();
    }

    @Test
    void aServiceCannotBeInvoicedTwiceWhileTheFirstInvoiceStands() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        issueFor(procedure);

        Result<?> second =
                root.issueInvoice()
                        .execute(
                                new IssueInvoice.Command(
                                        cashier,
                                        procedure.id(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) second).messageKey()).isEqualTo("invoice.already_issued");
    }

    @Test
    void everyFinancialStepLeavesAnAuditEntry() {
        Person citizen = registerCitizen();
        Procedure procedure = openCaseFor(citizen);
        satisfyCedula(procedure);
        openDrawer();
        Invoice invoice = issueFor(procedure);
        RegisterPayment.Receipt receipt = pay(invoice, dop("500.00"));
        root.refundPayment()
                .execute(
                        new RefundPayment.Command(
                                cashier, receipt.payment().id(), Optional.empty(), "Duplicado", "test"))
                .orElseThrow();

        List<String> actions =
                root.auditTrail()
                        .search(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                200,
                                0)
                        .stream()
                        .map(org.sirmax.domain.audit.AuditEvent::action)
                        .toList();

        assertThat(actions)
                .contains(
                        "cash.session_opened",
                        "invoice.issued",
                        "payment.registered",
                        "payment.refunded");
    }

    private RegisterPayment.Receipt pay(Invoice invoice, Money amount) {
        return root.registerPayment()
                .execute(
                        new RegisterPayment.Command(
                                cashier,
                                invoice.id(),
                                PaymentMethod.CASH,
                                amount,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
    }

    private void advance(Procedure procedure) {
        root.advanceProcedure()
                .execute(
                        new AdvanceProcedure.Command(
                                cashier, procedure.id(), TransitionKind.ADVANCE, Optional.empty(), "test"));
    }
}
