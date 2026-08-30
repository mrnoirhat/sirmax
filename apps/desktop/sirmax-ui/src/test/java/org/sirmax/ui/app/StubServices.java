// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

/**
 * An {@link AppServices} that answers only what the login screen asks and throws for everything
 * else.
 *
 * <p>Throwing rather than returning null is deliberate: a UI test that accidentally reaches for the
 * database should fail loudly at the call, not several frames later with a NullPointerException in
 * a place that has nothing to do with the mistake.
 */
final class StubServices implements AppServices {

    private final boolean firstRun;

    StubServices(boolean firstRun) {
        this.firstRun = firstRun;
    }

    @Override
    public boolean needsInitialSetup() {
        return firstRun;
    }

    @Override
    public org.sirmax.application.usecase.ProvisionInitialAdmin provisionInitialAdmin() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.Authenticate authenticate() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.RegisterPerson registerPerson() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.FindDuplicatePeople findDuplicatePeople() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.PersonRepository people() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.ServiceCatalogRepository serviceCatalog() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.SeedServiceCatalog seedServiceCatalog() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.CreateServiceDraft createServiceDraft() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.ConfigureServiceDraft configureServiceDraft() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.PublishServiceVersion publishServiceVersion() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.CreateServiceDraftVersion createServiceDraftVersion() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.SetServiceAvailability setServiceAvailability() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.OrganizationRepository organization() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.SettingsRepository settings() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.StartProcedure startProcedure() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.UpdateProcedureRequirement updateProcedureRequirement() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.SaveProcedureForm saveProcedureForm() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.AdvanceProcedure advanceProcedure() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.AssignProcedure assignProcedure() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.AddProcedureNote addProcedureNote() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.ProcedureRepository procedures() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.UserRepository users() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.IssueInvoice issueInvoice() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.RegisterPayment registerPayment() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.VoidInvoice voidInvoice() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.RefundPayment refundPayment() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.ManageCashSession manageCashSession() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.BillingRepository billing() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.AuditRepository auditTrail() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.GrantAgreement grantAgreement() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.TransferAgreement transferAgreement() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.RegisterDocument registerDocument() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.ConductInspection conductInspection() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.AssetRepository assets() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.RegistryRepository registry() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.IssueDocument issueDocument() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.PrintDocument printDocument() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.DocumentRepository documents() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.DocumentPrinter printer() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.CreateBackup createBackup() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.RestoreBackup restoreBackup() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.ManageBackupPolicy manageBackupPolicy() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.BackupRepository backups() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.CloudBackupTarget cloudBackups() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.usecase.VerifyAuditIntegrity verifyAuditIntegrity() {
        throw notNeeded();
    }

    @Override
    public org.sirmax.application.port.SecurityPolicyRepository securityPolicy() {
        throw notNeeded();
    }

    private static UnsupportedOperationException notNeeded() {
        return new UnsupportedOperationException("The login screen must not reach for this");
    }
}
