// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import org.sirmax.application.port.AuditRepository;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.OrganizationPartyRepository;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.port.ProcedureFinance;
import org.sirmax.application.port.ProcedureRepository;
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
import org.sirmax.application.usecase.ConfigureServiceDraft;
import org.sirmax.application.usecase.CreateServiceDraft;
import org.sirmax.application.usecase.CreateServiceDraftVersion;
import org.sirmax.application.usecase.FindDuplicatePeople;
import org.sirmax.application.usecase.IssueInvoice;
import org.sirmax.application.usecase.ManageCashSession;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RefundPayment;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.PublishServiceVersion;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.SaveProcedureForm;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.SetServiceAvailability;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.VoidInvoice;
import org.sirmax.application.usecase.UpdateProcedureRequirement;
import org.sirmax.infrastructure.AppPaths;
import org.sirmax.infrastructure.UuidV7IdGenerator;
import org.sirmax.infrastructure.persistence.JdbcUnitOfWork;
import org.sirmax.infrastructure.persistence.SqliteBillingRepository;
import org.sirmax.infrastructure.persistence.JsonServiceCatalogTemplateSource;
import org.sirmax.infrastructure.persistence.SqliteAuditRepository;
import org.sirmax.infrastructure.persistence.SqliteAuditSink;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.infrastructure.persistence.SqliteIdentificationRepository;
import org.sirmax.infrastructure.persistence.SqliteOrganizationPartyRepository;
import org.sirmax.infrastructure.persistence.SqliteNumberingRepository;
import org.sirmax.infrastructure.persistence.SqliteOrganizationRepository;
import org.sirmax.infrastructure.persistence.SqlitePersonRepository;
import org.sirmax.infrastructure.persistence.SqliteProcedureRepository;
import org.sirmax.infrastructure.persistence.SqliteRoleRepository;
import org.sirmax.infrastructure.persistence.SqliteServiceCatalogRepository;
import org.sirmax.infrastructure.persistence.SqliteSettingsRepository;
import org.sirmax.infrastructure.persistence.SqliteUserRepository;
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

    private CompositionRoot(SqliteDatabase database) {
        this.database = database;
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
        this.unitOfWork = new JdbcUnitOfWork(database);
        this.audit = new Audit(new SqliteAuditSink(database), clock, ids);

        this.authenticate =
                new Authenticate(userRepository, roleRepository, passwordHasher, ids, clock, audit);
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
    }

    /** Wire against the on-disk database under the platform data directory. */
    public static CompositionRoot bootstrapDefault() {
        return new CompositionRoot(SqliteDatabase.openAt(AppPaths.resolveDefault().databaseFile()));
    }

    /** Wire against an already-open database (tests pass an in-memory one). */
    public static CompositionRoot bootstrap(SqliteDatabase database) {
        return new CompositionRoot(database);
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

    public CreateServiceDraft createServiceDraft() {
        return createServiceDraft;
    }

    public ConfigureServiceDraft configureServiceDraft() {
        return configureServiceDraft;
    }

    public PublishServiceVersion publishServiceVersion() {
        return publishServiceVersion;
    }

    public CreateServiceDraftVersion createServiceDraftVersion() {
        return createServiceDraftVersion;
    }

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
    public AuditRepository auditTrail() {
        return auditRepository;
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

    public IdGenerator ids() {
        return ids;
    }

    @Override
    public void close() {
        database.close();
    }
}
