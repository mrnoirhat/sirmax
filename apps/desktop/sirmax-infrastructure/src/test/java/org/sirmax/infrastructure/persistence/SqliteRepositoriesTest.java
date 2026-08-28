// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.audit.AuditEvent;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.security.Role;

class SqliteRepositoriesTest {

    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    private SqliteDatabase db;
    private SqliteOrganizationRepository orgs;
    private SqliteUserRepository users;
    private SqliteRoleRepository roles;
    private SqlitePersonRepository people;
    private SqliteIdentificationRepository idents;

    @BeforeEach
    void setUp() {
        db = SqliteDatabase.openInMemory();
        db.migrate();
        orgs = new SqliteOrganizationRepository(db);
        users = new SqliteUserRepository(db);
        roles = new SqliteRoleRepository(db);
        people = new SqlitePersonRepository(db);
        idents = new SqliteIdentificationRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void seededAdministratorRoleHasEveryPermission() {
        Role admin = roles.findByName("administrador").orElseThrow();
        assertThat(admin.isSystem()).isTrue();
        assertThat(admin.permissions()).hasSize(Permission.values().length);
        assertThat(roles.list()).hasSize(4);
    }

    @Test
    void organizationUnitProfileAndDepartmentRoundTrip() {
        OrganizationUnit ou = OrganizationUnit.create("o1", "Ayto de Prueba", "Prueba", "DO", NOW);
        orgs.save(ou);
        InstitutionProfile.Overrides ov = new InstitutionProfile.Overrides();
        ov.phone = "809-555-0000";
        orgs.saveProfile(InstitutionProfile.empty("o1").with(ov));
        orgs.save(Department.create("d1", "o1", "Registro Civil", "REGCIV", NOW));

        assertThat(orgs.findActive()).map(OrganizationUnit::name).contains("Ayto de Prueba");
        assertThat(orgs.findProfile("o1")).flatMap(InstitutionProfile::phone).contains("809-555-0000");
        assertThat(orgs.findDepartmentByCode("o1", "regciv")).map(Department::name)
                .contains("Registro Civil");
        assertThat(orgs.listActiveDepartments("o1")).hasSize(1);
    }

    @Test
    void userSaveFindRolesAndCount() {
        AppUser u =
                AppUser.create(
                        "u1", "Cajera1", "Cajera Uno",
                        new PasswordHash("PBKDF2-HMAC-SHA256", "enc"), null, NOW);
        users.save(u);
        String cashierRoleId = roles.findByName("CAJERA").orElseThrow().id();
        users.replaceRoles("u1", Set.of(cashierRoleId));

        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByUsername("cajera1")).map(AppUser::displayName).contains("Cajera Uno");
        assertThat(users.roleIdsOf("u1")).containsExactly(cashierRoleId);
        assertThat(roles.rolesOf("u1")).singleElement().satisfies(
                r -> assertThat(r.grants(Permission.PAYMENT_REGISTER)).isTrue());

        // upsert path
        u.rename("Cajera Renombrada", NOW.plusSeconds(60));
        users.save(u);
        assertThat(users.findById("u1")).map(AppUser::displayName).contains("Cajera Renombrada");
    }

    @Test
    void personSearchIsCaseInsensitiveContainsAndPaginates() {
        people.save(Person.create("p1", new PersonName("Ana María", "Reyes Cruz"), NOW));
        people.save(Person.create("p2", new PersonName("Juan", "Pérez"), NOW.plusSeconds(1)));
        people.save(Person.create("p3", new PersonName("Ana", "Gómez"), NOW.plusSeconds(2)));

        assertThat(people.search("ana", 10, 0)).hasSize(2);
        assertThat(people.countSearch("reyes")).isEqualTo(1);
        assertThat(people.search("", 2, 0)).hasSize(2);
        assertThat(people.search("", 2, 2)).hasSize(1);
        assertThat(people.findById("p1")).map(Person::fullName).contains("Ana María Reyes Cruz");
    }

    @Test
    void identificationSaveLookupAndByOwner() {
        Person p = Person.create("p1", new PersonName("Luis", "Cruz"), NOW);
        people.save(p);
        idents.save(
                org.sirmax.domain.identity.Identification.of(
                                "i1", p.ref(), IdentificationType.CEDULA, "001-1234567-8", NOW)
                        .asPrimary());

        assertThat(idents.findByNumber(IdentificationType.CEDULA, "001-1234567-8")).isPresent();
        assertThat(idents.findByNumber(IdentificationType.RNC, "001-1234567-8")).isEmpty();
        assertThat(idents.forOwner(p.ref())).singleElement()
                .satisfies(i -> assertThat(i.primary()).isTrue());
    }

    @Test
    void auditSinkWritesAndTheTableStaysAppendOnly() {
        SqliteAuditSink sink = new SqliteAuditSink(db);
        sink.record(
                new AuditEvent(
                        "e1",
                        NOW,
                        Optional.of("u1"),
                        "person.created",
                        "Person",
                        "p1",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        "s1",
                        "test"));

        assertThat(JdbcHelper.queryLong(db.connection(), "SELECT count(*) FROM audit_event"))
                .isEqualTo(1);
        assertThat(
                        JdbcHelper.queryOne(
                                db.connection(),
                                "SELECT action FROM audit_event WHERE id = 'e1'",
                                rs -> rs.getString(1)))
                .contains("person.created");
    }
}
