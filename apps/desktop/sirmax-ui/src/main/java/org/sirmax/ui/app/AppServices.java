// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import org.sirmax.application.port.AssetRepository;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.application.port.CloudBackupTarget;
import org.sirmax.application.port.AuditRepository;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.DocumentPrinter;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.RegistryRepository;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.application.port.SecurityPolicyRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.SettingsRepository;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.usecase.AddProcedureNote;
import org.sirmax.application.usecase.AdvanceProcedure;
import org.sirmax.application.usecase.AssignProcedure;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ConductInspection;
import org.sirmax.application.usecase.ConfigureServiceDraft;
import org.sirmax.application.usecase.CreateBackup;
import org.sirmax.application.usecase.CreateServiceDraft;
import org.sirmax.application.usecase.CreateServiceDraftVersion;
import org.sirmax.application.usecase.FindDuplicatePeople;
import org.sirmax.application.usecase.GrantAgreement;
import org.sirmax.application.usecase.IssueDocument;
import org.sirmax.application.usecase.IssueInvoice;
import org.sirmax.application.usecase.ManageBackupPolicy;
import org.sirmax.application.usecase.ManageCashSession;
import org.sirmax.application.usecase.PrintDocument;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.PublishServiceVersion;
import org.sirmax.application.usecase.RefundPayment;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.RegisterDocument;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.RestoreBackup;
import org.sirmax.application.usecase.SaveProcedureForm;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.SetServiceAvailability;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.TransferAgreement;
import org.sirmax.application.usecase.UpdateProcedureRequirement;
import org.sirmax.application.usecase.VerifyAuditIntegrity;
import org.sirmax.application.usecase.VoidInvoice;

/**
 * What the UI is allowed to reach for.
 *
 * <p>The shell and its views are constructed against this interface, not against the composition
 * root: {@code sirmax-ui} must not know that SQLite, Jackson or a {@code CompositionRoot} exist. The
 * composition root in {@code sirmax-app} implements it, which keeps the dependency arrow pointing
 * the way ArchUnit enforces (ADR 0005).
 *
 * <p>Read-only repositories appear here too. Listing the service catalog or a worklist is a query,
 * not a state change, and wrapping every list in a use case would add a layer that only forwards.
 */
public interface AppServices {

    // ── identity / bootstrap ──
    boolean needsInitialSetup();

    ProvisionInitialAdmin provisionInitialAdmin();

    Authenticate authenticate();

    // ── citizens ──
    RegisterPerson registerPerson();

    FindDuplicatePeople findDuplicatePeople();

    org.sirmax.application.port.PersonRepository people();

    // ── service catalog ──
    ServiceCatalogRepository serviceCatalog();

    SeedServiceCatalog seedServiceCatalog();

    /**
     * Authoring a service (§22, §55). A definition is created as a draft, configured while it stays
     * a draft, then published — and from then on it is immutable, because live procedures pin the
     * version they were started under (§39). Changing a published service means a new draft version,
     * never an edit.
     */
    CreateServiceDraft createServiceDraft();

    ConfigureServiceDraft configureServiceDraft();

    PublishServiceVersion publishServiceVersion();

    CreateServiceDraftVersion createServiceDraftVersion();

    SetServiceAvailability setServiceAvailability();

    // ── organization ──

    /** Institution profile and departments, for the settings and departments screens. */
    OrganizationRepository organization();

    /** Operator preferences that are not domain data (theme, printing defaults). */
    SettingsRepository settings();

    // ── cases ──
    StartProcedure startProcedure();

    UpdateProcedureRequirement updateProcedureRequirement();

    SaveProcedureForm saveProcedureForm();

    AdvanceProcedure advanceProcedure();

    AssignProcedure assignProcedure();

    AddProcedureNote addProcedureNote();

    ProcedureRepository procedures();

    UserRepository users();

    /** So the personnel list can name a user's roles instead of printing their ids. */
    RoleRepository roles();

    // ── billing, payments and cash ──
    IssueInvoice issueInvoice();

    RegisterPayment registerPayment();

    VoidInvoice voidInvoice();

    RefundPayment refundPayment();

    ManageCashSession manageCashSession();

    BillingRepository billing();

    /** Read-only audit trail; every consumer must gate on {@code audit.read} first. */
    AuditRepository auditTrail();

    // ── municipal modules ──
    GrantAgreement grantAgreement();

    TransferAgreement transferAgreement();

    RegisterDocument registerDocument();

    ConductInspection conductInspection();

    AssetRepository assets();

    RegistryRepository registry();

    // ── documents, PDF and printing ──
    IssueDocument issueDocument();

    PrintDocument printDocument();

    DocumentRepository documents();

    /** Only the printer-profile screen needs this, to list the workstation's Windows queues. */
    DocumentPrinter printer();

    // ── backup and recovery ──
    CreateBackup createBackup();

    RestoreBackup restoreBackup();

    ManageBackupPolicy manageBackupPolicy();

    BackupRepository backups();

    /** So the settings screen can say whether an account is connected, and to which folder. */
    CloudBackupTarget cloudBackups();

    // ── security ──
    VerifyAuditIntegrity verifyAuditIntegrity();

    SecurityPolicyRepository securityPolicy();
}
