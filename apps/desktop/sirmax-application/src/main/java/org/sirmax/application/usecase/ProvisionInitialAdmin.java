// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.Set;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.AuditContext;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Role;
import org.sirmax.shared.Result;

/**
 * First-run setup: create the {@link OrganizationUnit} and the initial administrator account.
 *
 * <p>Refuses to run once any user exists — there is no other bootstrap path, and re-running would be
 * a privilege-escalation hole. No permission check applies (there are no users yet); the action is
 * audited as a system event.
 */
public final class ProvisionInitialAdmin
        implements UseCase<ProvisionInitialAdmin.Command, ProvisionInitialAdmin.Provisioned> {

    /** @param adminPassword wiped by this use case after hashing */
    public record Command(
            String organizationName,
            String municipality,
            String country,
            String adminUsername,
            String adminDisplayName,
            char[] adminPassword) {}

    public record Provisioned(String organizationUnitId, String adminUserId) {}

    private final UserRepository users;
    private final RoleRepository roles;
    private final OrganizationRepository organizations;
    private final PasswordHasher hasher;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public ProvisionInitialAdmin(
            UserRepository users,
            RoleRepository roles,
            OrganizationRepository organizations,
            PasswordHasher hasher,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.users = users;
        this.roles = roles;
        this.organizations = organizations;
        this.hasher = hasher;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Provisioned> execute(Command command) {
        if (users.count() > 0) {
            return Result.err("ALREADY_PROVISIONED", "provision.already_done");
        }
        Role adminRole = roles.findByName("ADMINISTRADOR").orElse(null);
        if (adminRole == null) {
            return Result.err("SYSTEM_ROLE_MISSING", "provision.system_role_missing");
        }
        if (command.adminPassword() == null || command.adminPassword().length < 8) {
            return Result.err("WEAK_PASSWORD", "provision.weak_password");
        }

        try {
            return Result.ok(unitOfWork.execute(() -> doProvision(command, adminRole)));
        } finally {
            java.util.Arrays.fill(command.adminPassword(), '\0');
        }
    }

    private Provisioned doProvision(Command command, Role adminRole) {
        Instant now = clock.now();
        AuditContext ctx = AuditContext.system("first-run");

        OrganizationUnit unit =
                OrganizationUnit.create(
                        ids.newId(),
                        command.organizationName(),
                        command.municipality(),
                        command.country(),
                        now);
        organizations.save(unit);
        organizations.saveProfile(InstitutionProfile.empty(unit.id()));
        audit.record(ctx, "organization.provisioned", "OrganizationUnit", unit.id());

        PasswordHash hash = hasher.hash(command.adminPassword());
        AppUser admin =
                AppUser.create(
                        ids.newId(),
                        command.adminUsername(),
                        command.adminDisplayName(),
                        hash,
                        null,
                        now);
        users.save(admin);
        users.replaceRoles(admin.id(), Set.of(adminRole.id()));
        audit.record(ctx, "user.created", "AppUser", admin.id());

        return new Provisioned(unit.id(), admin.id());
    }
}
