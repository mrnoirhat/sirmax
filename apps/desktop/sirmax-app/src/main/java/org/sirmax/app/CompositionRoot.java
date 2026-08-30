// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import org.sirmax.application.port.AssetRepository;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.application.port.AuditRepository;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.CloudBackupTarget;
import org.sirmax.application.port.DocumentPrinter;
import org.sirmax.application.port.DocumentRenderer;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.OrganizationPartyRepository;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.port.ProcedureFinance;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.RegistryRepository;
import org.sirmax.application.port.SecurityPolicyRepository;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.ServiceCatalogTemplateSource;
import org.sirmax.application.port.SettingsRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.security.Audit;
import org.sirmax.application.usecase.AddProcedureNote;
import org.sirmax.application.usecase.AdvanceProcedure;
import org.sirmax.application.usecase.AssignProcedure;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ConductInspection;
import org.sirmax.application.usecase.CreateBackup;
import org.sirmax.application.usecase.ConfigureServiceDraft;
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
import org.sirmax.application.usecase.RefundPayment;
import org.sirmax.application.usecase.RegisterDocument;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.PublishServiceVersion;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.RestoreBackup;
import org.sirmax.application.usecase.SaveProcedureForm;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.SetServiceAvailability;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.VerifyAuditIntegrity;
import org.sirmax.application.usecase.TransferAgreement;
import org.sirmax.application.usecase.VoidInvoice;
import org.sirmax.application.usecase.UpdateProcedureRequirement;
import org.sirmax.infrastructure.AppPaths;
import org.sirmax.infrastructure.UuidV7IdGenerator;
import org.sirmax.infrastructure.persistence.JdbcUnitOfWork;
import org.sirmax.infrastructure.persistence.SqliteBillingRepository;
import org.sirmax.infrastructure.persistence.JsonServiceCatalogTemplateSource;
import org.sirmax.infrastructure.backup.GoogleDriveBackupTarget;
import org.sirmax.infrastructure.backup.SecretStore;
import org.sirmax.infrastructure.backup.SqliteBackupEngine;
import org.sirmax.infrastructure.persistence.SqliteAssetRepository;
import org.sirmax.infrastructure.persistence.SqliteAuditRepository;
import org.sirmax.infrastructure.persistence.SqliteBackupRepository;
import org.sirmax.infrastructure.persistence.SqliteAuditSink;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.infrastructure.persistence.SqliteDocumentRepository;
import org.sirmax.infrastructure.persistence.SqliteIdentificationRepository;
import org.sirmax.infrastructure.persistence.SqliteOrganizationPartyRepository;
import org.sirmax.infrastructure.persistence.SqliteNumberingRepository;
import org.sirmax.infrastructure.persistence.SqliteOrganizationRepository;
import org.sirmax.infrastructure.persistence.SqlitePersonRepository;
import org.sirmax.infrastructure.persistence.SqliteProcedureRepository;
import org.sirmax.infrastructure.persistence.SqliteRegistryRepository;
import org.sirmax.infrastructure.persistence.SqliteRoleRepository;
import org.sirmax.infrastructure.persistence.SqliteSecurityPolicyRepository;
import org.sirmax.infrastructure.persistence.SqliteServiceCatalogRepository;
import org.sirmax.infrastructure.persistence.SqliteSettingsRepository;
import org.sirmax.infrastructure.persistence.SqliteUserRepository;
import org.sirmax.infrastructure.print.JavaPrintServiceDocumentPrinter;
import org.sirmax.infrastructure.print.PdfDocumentRenderer;
import org.sirmax.infrastructure.security.Pbkdf2PasswordHasher;
import org.sirmax.infrastructure.time.SystemClock;
import org.sirmax.ui.app.AppServices;

/**
 * Hand-wired dependency graph for the desktop client (no DI container — see {@code
 * docs/adr/0005-modular-domain-architecture.md}).
 *
 * <p>Owns the {@link SqliteDatabase} (migrated on start-up) and constructs the infrastructure
 * adapters, the {@link Audit} helper and the use cases. The UI (Phase 5) reads from here.
 */
public final class CompositionRoot implements AppServices, AutoCloseable {

    private final SqliteDatabase database;

    private final Clock clock = new SystemClock();
    private final IdGenerator ids = new UuidV7IdGenerator();
    private final PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationPartyRepository organizationPartyRepository;
    private final PersonRepository personRepository;
    private final IdentificationRepository identificationRepository;
    private final SettingsRepository settingsRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceCatalogTemplateSource serviceCatalogTemplateSource;
    private final ProcedureRepository procedureRepository;
    private final NumberingRepository numberingRepository;
    private final BillingRepository billingRepository;
    private final AuditRepository auditRepository;
    private final SecurityPolicyRepository securityPolicyRepository;
    private final AssetRepository assetRepository;
    private final RegistryRepository registryRepository;
    private final DocumentRepository documentRepository;
    private final BackupRepository backupRepository;
    private final BackupEngine backupEngine;
    private final CloudBackupTarget cloudBackupTarget;
    private final DocumentRenderer documentRenderer;
    private final DocumentPrinter documentPrinter;
    private final ProcedureFinance procedureFinance;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    private final Authenticate authenticate;
    private final ProvisionInitialAdmin provisionInitialAdmin;
    private final RegisterPerson registerPerson;
    private final CreateServiceDraft createServiceDraft;
    private final ConfigureServiceDraft configureServiceDraft;
    private final PublishServiceVersion publishServiceVersion;
    private final CreateServiceDraftVersion createServiceDraftVersion;
    private final SetServiceAvailability setServiceAvailability;
    private final SeedServiceCatalog seedServiceCatalog;
    private final StartProcedure startProcedure;
    private final UpdateProcedureRequirement updateProcedureRequirement;
    private final SaveProcedureForm saveProcedureForm;
    private final AdvanceProcedure advanceProcedure;
    private final AssignProcedure assignProcedure;
    private final AddProcedureNote addProcedureNote;
    private final FindDuplicatePeople findDuplicatePeople;
    private final IssueInvoice issueInvoice;
    private final RegisterPayment registerPayment;
    private final VoidInvoice voidInvoice;
    private final RefundPayment refundPayment;
    private final ManageCashSession manageCashSession;
    private final GrantAgreement grantAgreement;
    private final TransferAgreement transferAgreement;
    private final RegisterDocument registerDocument;
    private final ConductInspection conductInspection;
    private final IssueDocument issueDocument;
    private final PrintDocument printDocument;
    private final CreateBackup createBackup;
    private final RestoreBackup restoreBackup;
    private final ManageBackupPolicy manageBackupPolicy;
    private final VerifyAuditIntegrity verifyAuditIntegrity;

    private final AppPaths paths;

    private CompositionRoot(SqliteDatabase database, AppPaths paths) {
        this.database = database;
        this.paths = paths;
        database.migrate();

        this.userRepository = new SqliteUserRepository(database);
        this.roleRepository = new SqliteRoleRepository(database);
        this.organizationRepository = new SqliteOrganizationRepository(database);
        this.organizationPartyRepository = new SqliteOrganizationPartyRepository(database);
        this.personRepository = new SqlitePersonRepository(database);
        this.identificationRepository = new SqliteIdentificationRepository(database);
        this.settingsRepository = new SqliteSettingsRepository(database);
        this.serviceCatalogRepository = new SqliteServiceCatalogRepository(database);
        this.serviceCatalogTemplateSource = new JsonServiceCatalogTemplateSource();
        this.procedureRepository = new SqliteProcedureRepository(database);
        this.numberingRepository = new SqliteNumberingRepository(database, clock);
        SqliteBillingRepository billing = new SqliteBillingRepository(database);
        this.billingRepository = billing;
        // The same adapter answers the workflow's payment checkpoint, straight from the invoice
        // table — no billing state is duplicated onto the case.
        this.procedureFinance = billing;
        this.auditRepository = new SqliteAuditRepository(database);
        this.securityPolicyRepository = new SqliteSecurityPolicyRepository(database);
        this.assetRepository = new SqliteAssetRepository(database);
        this.registryRepository = new SqliteRegistryRepository(database);
        this.documentRepository = new SqliteDocumentRepository(database);
        this.documentRenderer = new PdfDocumentRenderer();
        this.backupRepository = new SqliteBackupRepository(database);
        this.backupEngine =
                new SqliteBackupEngine(database, paths.backupsDir(), paths.databaseFile());
        this.cloudBackupTarget = new GoogleDriveBackupTarget(new SecretStore(paths.dataDir()));
        this.documentPrinter = new JavaPrintServiceDocumentPrinter();
        this.unitOfWork = new JdbcUnitOfWork(database);
        this.audit = new Audit(new SqliteAuditSink(database), clock, ids);

        this.authenticate =
                new Authenticate(
                        userRepository,
                        roleRepository,
                        passwordHasher,
                        ids,
                        clock,
                        audit,
                        securityPolicyRepository);
        this.provisionInitialAdmin =
                new ProvisionInitialAdmin(
                        userRepository,
                        roleRepository,
                        organizationRepository,
                        passwordHasher,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.registerPerson =
                new RegisterPerson(
                        personRepository, identificationRepository, ids, clock, unitOfWork, audit);

        this.createServiceDraft =
                new CreateServiceDraft(serviceCatalogRepository, ids, clock, unitOfWork, audit);
        this.configureServiceDraft =
                new ConfigureServiceDraft(serviceCatalogRepository, unitOfWork, audit);
        this.publishServiceVersion =
                new PublishServiceVersion(serviceCatalogRepository, clock, unitOfWork, audit);
        this.createServiceDraftVersion =
                new CreateServiceDraftVersion(serviceCatalogRepository, ids, clock, unitOfWork, audit);
        this.setServiceAvailability =
                new SetServiceAvailability(serviceCatalogRepository, clock, unitOfWork, audit);
        this.seedServiceCatalog =
                new SeedServiceCatalog(
                        serviceCatalogTemplateSource,
                        serviceCatalogRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);

        this.startProcedure =
                new StartProcedure(
                        procedureRepository,
                        serviceCatalogRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.updateProcedureRequirement =
                new UpdateProcedureRequirement(procedureRepository, ids, clock, unitOfWork, audit);
        this.saveProcedureForm =
                new SaveProcedureForm(
                        procedureRepository, serviceCatalogRepository, ids, clock, unitOfWork, audit);
        this.advanceProcedure =
                new AdvanceProcedure(
                        procedureRepository,
                        serviceCatalogRepository,
                        procedureFinance,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.assignProcedure =
                new AssignProcedure(
                        procedureRepository, userRepository, ids, clock, unitOfWork, audit);
        this.addProcedureNote =
                new AddProcedureNote(procedureRepository, ids, clock, unitOfWork, audit);
        this.findDuplicatePeople =
                new FindDuplicatePeople(personRepository, identificationRepository);

        this.issueInvoice =
                new IssueInvoice(
                        billingRepository,
                        procedureRepository,
                        serviceCatalogRepository,
                        personRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.registerPayment =
                new RegisterPayment(
                        billingRepository,
                        procedureRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.voidInvoice = new VoidInvoice(billingRepository, clock, unitOfWork, audit);
        this.refundPayment =
                new RefundPayment(
                        billingRepository, numberingRepository, ids, clock, unitOfWork, audit);
        this.manageCashSession =
                new ManageCashSession(
                        billingRepository, numberingRepository, ids, clock, unitOfWork, audit);

        this.grantAgreement =
                new GrantAgreement(
                        assetRepository, numberingRepository, ids, clock, unitOfWork, audit);
        this.transferAgreement =
                new TransferAgreement(
                        assetRepository, numberingRepository, ids, clock, unitOfWork, audit);
        this.registerDocument =
                new RegisterDocument(
                        registryRepository,
                        procedureRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.conductInspection =
                new ConductInspection(
                        registryRepository,
                        procedureRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);

        this.issueDocument =
                new IssueDocument(
                        documentRepository,
                        billingRepository,
                        procedureRepository,
                        organizationRepository,
                        numberingRepository,
                        ids,
                        clock,
                        unitOfWork,
                        audit);
        this.printDocument =
                new PrintDocument(
                        documentRepository,
                        documentRenderer,
                        documentPrinter,
                        ids,
                        clock,
                        unitOfWork,
                        audit);

        this.createBackup =
                new CreateBackup(
                        backupRepository,
                        backupEngine,
                        cloudBackupTarget,
                        numberingRepository,
                        ids,
                        clock,
                        audit);
        this.restoreBackup =
                new RestoreBackup(backupRepository, backupEngine, createBackup, ids, clock, audit);
        this.manageBackupPolicy =
                new ManageBackupPolicy(
                        backupRepository, backupEngine, cloudBackupTarget, createBackup, clock, audit);
        this.verifyAuditIntegrity = new VerifyAuditIntegrity(auditRepository);
    }

    /** Wire against the on-disk database under the platform data directory. */
    public static CompositionRoot bootstrapDefault() {
        AppPaths paths = AppPaths.resolveDefault();
        return new CompositionRoot(SqliteDatabase.openAt(paths.databaseFile()), paths);
    }

    /** Wire against an already-open database (tests pass an in-memory one). */
    public static CompositionRoot bootstrap(SqliteDatabase database) {
        // Tests run against an in-memory database but still need somewhere real for archives.
        return new CompositionRoot(database, AppPaths.resolveDefault());
    }

    /** Wire against an already-open database with a specific data directory (backup tests). */
    public static CompositionRoot bootstrap(SqliteDatabase database, AppPaths paths) {
        return new CompositionRoot(database, paths);
    }

    @Override
    public boolean needsInitialSetup() {
        return userRepository.count() == 0;
    }

    @Override
    public Authenticate authenticate() {
        return authenticate;
    }

    @Override
    public ProvisionInitialAdmin provisionInitialAdmin() {
        return provisionInitialAdmin;
    }

    @Override
    public RegisterPerson registerPerson() {
        return registerPerson;
    }

    @Override
    public CreateServiceDraft createServiceDraft() {
        return createServiceDraft;
    }

    @Override
    public ConfigureServiceDraft configureServiceDraft() {
        return configureServiceDraft;
    }

    @Override
    public PublishServiceVersion publishServiceVersion() {
        return publishServiceVersion;
    }

    @Override
    public CreateServiceDraftVersion createServiceDraftVersion() {
        return createServiceDraftVersion;
    }

    @Override
    public SetServiceAvailability setServiceAvailability() {
        return setServiceAvailability;
    }

    @Override
    public SeedServiceCatalog seedServiceCatalog() {
        return seedServiceCatalog;
    }

    @Override
    public StartProcedure startProcedure() {
        return startProcedure;
    }

    @Override
    public UpdateProcedureRequirement updateProcedureRequirement() {
        return updateProcedureRequirement;
    }

    @Override
    public SaveProcedureForm saveProcedureForm() {
        return saveProcedureForm;
    }

    @Override
    public AdvanceProcedure advanceProcedure() {
        return advanceProcedure;
    }

    @Override
    public AssignProcedure assignProcedure() {
        return assignProcedure;
    }

    @Override
    public AddProcedureNote addProcedureNote() {
        return addProcedureNote;
    }

    @Override
    public FindDuplicatePeople findDuplicatePeople() {
        return findDuplicatePeople;
    }

    @Override
    public IssueInvoice issueInvoice() {
        return issueInvoice;
    }

    @Override
    public RegisterPayment registerPayment() {
        return registerPayment;
    }

    @Override
    public VoidInvoice voidInvoice() {
        return voidInvoice;
    }

    @Override
    public RefundPayment refundPayment() {
        return refundPayment;
    }

    @Override
    public ManageCashSession manageCashSession() {
        return manageCashSession;
    }

    @Override
    public BillingRepository billing() {
        return billingRepository;
    }

    @Override
    public VerifyAuditIntegrity verifyAuditIntegrity() {
        return verifyAuditIntegrity;
    }

    @Override
    public SecurityPolicyRepository securityPolicy() {
        return securityPolicyRepository;
    }

    @Override
    public AuditRepository auditTrail() {
        return auditRepository;
    }

    @Override
    public GrantAgreement grantAgreement() {
        return grantAgreement;
    }

    @Override
    public TransferAgreement transferAgreement() {
        return transferAgreement;
    }

    @Override
    public RegisterDocument registerDocument() {
        return registerDocument;
    }

    @Override
    public ConductInspection conductInspection() {
        return conductInspection;
    }

    @Override
    public IssueDocument issueDocument() {
        return issueDocument;
    }

    @Override
    public PrintDocument printDocument() {
        return printDocument;
    }

    @Override
    public CreateBackup createBackup() {
        return createBackup;
    }

    @Override
    public RestoreBackup restoreBackup() {
        return restoreBackup;
    }

    @Override
    public ManageBackupPolicy manageBackupPolicy() {
        return manageBackupPolicy;
    }

    @Override
    public BackupRepository backups() {
        return backupRepository;
    }

    @Override
    public CloudBackupTarget cloudBackups() {
        return cloudBackupTarget;
    }

    @Override
    public DocumentRepository documents() {
        return documentRepository;
    }

    @Override
    public DocumentPrinter printer() {
        return documentPrinter;
    }

    @Override
    public AssetRepository assets() {
        return assetRepository;
    }

    @Override
    public RegistryRepository registry() {
        return registryRepository;
    }

    public ProcedureRepository procedureRepository() {
        return procedureRepository;
    }

    public NumberingRepository numberingRepository() {
        return numberingRepository;
    }

    public ServiceCatalogRepository serviceCatalogRepository() {
        return serviceCatalogRepository;
    }

    // ── AppServices: names the UI reads by, kept short at the call site ──

    @Override
    public ServiceCatalogRepository serviceCatalog() {
        return serviceCatalogRepository;
    }

    @Override
    public OrganizationRepository organization() {
        return organizationRepository;
    }

    @Override
    public SettingsRepository settings() {
        return settingsRepository;
    }

    @Override
    public ProcedureRepository procedures() {
        return procedureRepository;
    }

    @Override
    public PersonRepository people() {
        return personRepository;
    }

    @Override
    public UserRepository users() {
        return userRepository;
    }

    public ServiceCatalogTemplateSource serviceCatalogTemplateSource() {
        return serviceCatalogTemplateSource;
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public RoleRepository roleRepository() {
        return roleRepository;
    }

    public OrganizationRepository organizationRepository() {
        return organizationRepository;
    }

    public OrganizationPartyRepository organizationPartyRepository() {
        return organizationPartyRepository;
    }

    public PersonRepository personRepository() {
        return personRepository;
    }

    public SettingsRepository settingsRepository() {
        return settingsRepository;
    }

    public Clock clock() {
        return clock;
    }

    /**
     * The audit helper. Exposed so a test can rebuild one use case around a double — printing is
     * the one adapter CI cannot exercise, since a headless runner has no printers.
     */
    public Audit auditFor() {
        return audit;
    }

    public IdGenerator ids() {
        return ids;
    }

    @Override
    public void close() {
        database.close();
    }
}
