// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.port.ProcedureFinance;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.FieldType;
import org.sirmax.domain.service.FormField;
import org.sirmax.domain.service.FormSchema;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.shared.Result;

/**
 * The Phase 5 counter loop end to end against fakes: open a case, be blocked by the checklist, clear
 * it, walk the workflow and decide — the "Escenario A" spine of the master prompt (§77).
 */
class FrontOfficeTest {

    private static final Instant NOW = Instant.parse("2026-03-05T13:00:00Z");

    private final Fakes.InMemoryServiceCatalog catalog = new Fakes.InMemoryServiceCatalog();
    private final Fakes.InMemoryProcedures procedures = new Fakes.InMemoryProcedures();
    private final Fakes.InMemoryUsers users = new Fakes.InMemoryUsers();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(NOW);
    private final Fakes.InMemoryNumbering numbering = new Fakes.InMemoryNumbering(clock);
    private final Fakes.RecordingAuditSink auditSink = new Fakes.RecordingAuditSink();
    private final Audit audit = new Audit(auditSink, clock, ids);
    private final Fakes.DirectUnitOfWork uow = new Fakes.DirectUnitOfWork();

    private StartProcedure start;
    private UpdateProcedureRequirement updateRequirement;
    private SaveProcedureForm saveForm;
    private AdvanceProcedure advance;
    private AssignProcedure assign;

    private Session clerk;
    private Session supervisor;

    @BeforeEach
    void setUp() {
        start = new StartProcedure(procedures, catalog, numbering, ids, clock, uow, audit);
        updateRequirement = new UpdateProcedureRequirement(procedures, ids, clock, uow, audit);
        saveForm = new SaveProcedureForm(procedures, catalog, ids, clock, uow, audit);
        advance =
                new AdvanceProcedure(
                        procedures, catalog, ProcedureFinance.unbilled(), ids, clock, uow, audit);
        assign = new AssignProcedure(procedures, users, ids, clock, uow, audit);

        AppUser clerkUser =
                AppUser.create("u1", "cajera", "Cajera", new PasswordHash("FAKE", "h:x"), null, NOW);
        AppUser supervisorUser =
                AppUser.create("u2", "jefa", "Jefa", new PasswordHash("FAKE", "h:x"), null, NOW);
        users.save(clerkUser);
        users.save(supervisorUser);
        clerk =
                new Session(
                        "s1",
                        clerkUser,
                        AccessPolicy.of(EnumSet.of(Permission.PROCEDURE_WORK)),
                        NOW);
        supervisor =
                new Session(
                        "s2",
                        supervisorUser,
                        AccessPolicy.of(
                                EnumSet.of(Permission.PROCEDURE_WORK, Permission.PROCEDURE_DECIDE)),
                        NOW);

        seedResidencyCertificate();
    }

    /** A free "certificado de residencia" with two requirements and a three-step workflow. */
    private void seedResidencyCertificate() {
        catalog.saveCategory(ServiceCategory.create("cat-1", "CERT", "Certificaciones", 1, NOW));

        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1",
                        "CERT-RES",
                        "cat-1",
                        "Certificado de residencia",
                        ServiceType.GRATUITO,
                        "DO",
                        NOW);
        definition.setCurrentVersion("ver-1", NOW);
        catalog.saveDefinition(definition);

        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, NOW);
        version.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument(
                                "cedula", "Cédula de identidad", RequirementStage.INTAKE),
                        new RequirementDef(
                                "carta_vecino",
                                "Carta de dos vecinos",
                                RequirementKind.DOCUMENT,
                                RequirementStage.REVIEW,
                                true,
                                Optional.empty(),
                                Optional.empty())));
        version.setFormSchema(
                new FormSchema(
                        List.of(
                                FormField.text("direccion", "Dirección", true),
                                new FormField(
                                        "anios_residiendo",
                                        "Años residiendo",
                                        FieldType.NUMBER,
                                        false,
                                        Optional.empty(),
                                        List.of()))));
        version.setWorkflow(
                new WorkflowDefinition(
                        "recepcion",
                        List.of(
                                WorkflowStep.task("recepcion", "Recepción", "revision"),
                                new WorkflowStep(
                                        "revision",
                                        "Revisión",
                                        StepType.REVIEW,
                                        Optional.empty(),
                                        2,
                                        List.of(Transition.advance("emision"))),
                                new WorkflowStep(
                                        "emision",
                                        "Emisión",
                                        StepType.DOCUMENT_OUTPUT,
                                        Optional.empty(),
                                        1,
                                        List.of(
                                                Transition.terminal(TransitionKind.APPROVE),
                                                Transition.terminal(TransitionKind.REJECT))))));
        version.setSla(Sla.businessDays(3));
        version.publish(NOW);
        assertThat(version.status()).isEqualTo(ServiceStatus.ACTIVE);
        catalog.saveVersion(version);
    }

    private Procedure openCase() {
        Result<Procedure> opened =
                start.execute(
                        new StartProcedure.Command(
                                clerk,
                                "svc-1",
                                PartyRef.person("per-1"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"));
        assertThat(opened.isOk()).isTrue();
        return opened.orElseThrow();
    }

    @Test
    void openingACaseNumbersItMaterializesTheChecklistAndSetsTheDueDate() {
        Procedure p = openCase();

        assertThat(p.code()).isEqualTo("TRM-2026-000001");
        assertThat(p.currentStepKey()).contains("recepcion");
        // opened Thursday 5 March + 3 business days = Tuesday 10 March
        assertThat(p.dueDate()).contains(java.time.LocalDate.of(2026, 3, 10));
        assertThat(procedures.findRequirements(p.id())).hasSize(2);
        assertThat(auditSink.actions()).contains("procedure.opened");
    }

    @Test
    void eachCaseGetsItsOwnNumber() {
        assertThat(openCase().code()).isEqualTo("TRM-2026-000001");
        assertThat(openCase().code()).isEqualTo("TRM-2026-000002");
    }

    @Test
    void aMissingRequirementBlocksTheAdvanceAndParksTheCase() {
        Procedure p = openCase();

        Result<Procedure> blocked =
                advance.execute(
                        new AdvanceProcedure.Command(
                                clerk, p.id(), TransitionKind.ADVANCE, Optional.empty(), "test"));

        assertThat(((Result.Err<?>) blocked).messageKey())
                .isEqualTo("procedure.requirements_incomplete");
        assertThat(procedures.findById(p.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.WAITING_REQUIREMENTS);
    }

    @Test
    void tickingTheLastRequirementUnblocksTheCaseAndReportsTheRemainingCount() {
        Procedure p = openCase();

        var afterFirst =
                updateRequirement
                        .execute(
                                new UpdateProcedureRequirement.Command(
                                        clerk,
                                        p.id(),
                                        "cedula",
                                        UpdateProcedureRequirement.Action.SATISFY,
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(afterFirst.pendingCount()).isEqualTo(1);
        assertThat(procedures.findById(p.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.WAITING_REQUIREMENTS);

        var afterSecond =
                updateRequirement
                        .execute(
                                new UpdateProcedureRequirement.Command(
                                        clerk,
                                        p.id(),
                                        "carta_vecino",
                                        UpdateProcedureRequirement.Action.SATISFY,
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(afterSecond.isComplete()).isTrue();
        assertThat(procedures.findById(p.id()).orElseThrow().status())
                .isEqualTo(ProcedureStatus.IN_PROGRESS);
    }

    @Test
    void onlyASupervisorCanWaiveAndAWaiverNeedsAReason() {
        Procedure p = openCase();

        Result<?> asClerk =
                updateRequirement.execute(
                        new UpdateProcedureRequirement.Command(
                                clerk,
                                p.id(),
                                "cedula",
                                UpdateProcedureRequirement.Action.WAIVE,
                                Optional.of("La trae mañana"),
                                "test"));
        assertThat(((Result.Err<?>) asClerk).messageKey()).isEqualTo("error.forbidden");

        Result<?> withoutReason =
                updateRequirement.execute(
                        new UpdateProcedureRequirement.Command(
                                supervisor,
                                p.id(),
                                "cedula",
                                UpdateProcedureRequirement.Action.WAIVE,
                                Optional.empty(),
                                "test"));
        assertThat(((Result.Err<?>) withoutReason).messageKey())
                .isEqualTo("procedure.waiver_needs_reason");

        var checklist =
                updateRequirement
                        .execute(
                                new UpdateProcedureRequirement.Command(
                                        supervisor,
                                        p.id(),
                                        "cedula",
                                        UpdateProcedureRequirement.Action.WAIVE,
                                        Optional.of("Presentó acta de nacimiento"),
                                        "test"))
                        .orElseThrow();
        assertThat(checklist.pendingCount()).isEqualTo(1);
    }

    @Test
    void theFormIsValidatedAgainstTheVersionSchema() {
        Procedure p = openCase();

        List<String> unknownField =
                saveForm
                        .execute(
                                new SaveProcedureForm.Command(
                                        clerk, p.id(), Map.of("no_existe", "x"), "test"))
                        .orElseThrow();
        assertThat(unknownField).contains("form.unknown_field:no_existe");

        List<String> badNumber =
                saveForm
                        .execute(
                                new SaveProcedureForm.Command(
                                        clerk,
                                        p.id(),
                                        Map.of("direccion", "C/ Duarte 12", "anios_residiendo", "x"),
                                        "test"))
                        .orElseThrow();
        assertThat(badNumber).containsExactly("form.not_a_number:anios_residiendo");

        List<String> clean =
                saveForm
                        .execute(
                                new SaveProcedureForm.Command(
                                        clerk,
                                        p.id(),
                                        Map.of("direccion", "C/ Duarte 12", "anios_residiendo", "7"),
                                        "test"))
                        .orElseThrow();
        assertThat(clean).isEmpty();
        assertThat(procedures.findFormValues(p.id())).containsEntry("direccion", "C/ Duarte 12");
    }

    @Test
    void aCompleteCaseWalksTheWorkflowAndOnlyASupervisorCanApprove() {
        Procedure p = openCase();
        satisfyEverything(p);

        assertThat(
                        advance.execute(
                                        new AdvanceProcedure.Command(
                                                clerk,
                                                p.id(),
                                                TransitionKind.ADVANCE,
                                                Optional.empty(),
                                                "test"))
                                .orElseThrow()
                                .currentStepKey())
                .contains("revision");
        assertThat(
                        advance.execute(
                                        new AdvanceProcedure.Command(
                                                clerk,
                                                p.id(),
                                                TransitionKind.ADVANCE,
                                                Optional.empty(),
                                                "test"))
                                .orElseThrow()
                                .currentStepKey())
                .contains("emision");

        Result<?> clerkApproves =
                advance.execute(
                        new AdvanceProcedure.Command(
                                clerk, p.id(), TransitionKind.APPROVE, Optional.empty(), "test"));
        assertThat(((Result.Err<?>) clerkApproves).messageKey()).isEqualTo("error.forbidden");

        Procedure approved =
                advance.execute(
                                new AdvanceProcedure.Command(
                                        supervisor,
                                        p.id(),
                                        TransitionKind.APPROVE,
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(approved.status()).isEqualTo(ProcedureStatus.APPROVED);
    }

    @Test
    void rejectingRequiresAReason() {
        Procedure p = openCase();
        satisfyEverything(p);
        advanceOnce(p);
        advanceOnce(p);

        Result<?> noReason =
                advance.execute(
                        new AdvanceProcedure.Command(
                                supervisor, p.id(), TransitionKind.REJECT, Optional.empty(), "test"));
        assertThat(((Result.Err<?>) noReason).messageKey())
                .isEqualTo("procedure.rejection_needs_reason");

        Procedure rejected =
                advance.execute(
                                new AdvanceProcedure.Command(
                                        supervisor,
                                        p.id(),
                                        TransitionKind.REJECT,
                                        Optional.of("Dirección fuera del municipio"),
                                        "test"))
                        .orElseThrow();
        assertThat(rejected.status()).isEqualTo(ProcedureStatus.REJECTED);
        assertThat(rejected.outcomeReason()).contains("Dirección fuera del municipio");
    }

    @Test
    void assigningRoutesTheCaseAndUnassigningReturnsItToTheQueue() {
        Procedure p = openCase();

        Procedure assigned =
                assign.execute(
                                new AssignProcedure.Command(
                                        supervisor,
                                        p.id(),
                                        Optional.of("dep-1"),
                                        Optional.of("u1"),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(assigned.assignedUserId()).contains("u1");

        Procedure unassigned =
                assign.execute(
                                new AssignProcedure.Command(
                                        supervisor,
                                        p.id(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(unassigned.assignedUserId()).isEmpty();
    }

    @Test
    void assigningToAnUnknownUserIsRefused() {
        Procedure p = openCase();

        Result<?> r =
                assign.execute(
                        new AssignProcedure.Command(
                                supervisor,
                                p.id(),
                                Optional.empty(),
                                Optional.of("nope"),
                                Optional.empty(),
                                "test"));

        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("user.not_found");
    }

    private void satisfyEverything(Procedure p) {
        for (String key : List.of("cedula", "carta_vecino")) {
            updateRequirement.execute(
                    new UpdateProcedureRequirement.Command(
                            clerk,
                            p.id(),
                            key,
                            UpdateProcedureRequirement.Action.SATISFY,
                            Optional.empty(),
                            "test"));
        }
    }

    private void advanceOnce(Procedure p) {
        advance.execute(
                new AdvanceProcedure.Command(
                        clerk, p.id(), TransitionKind.ADVANCE, Optional.empty(), "test"));
    }
}
