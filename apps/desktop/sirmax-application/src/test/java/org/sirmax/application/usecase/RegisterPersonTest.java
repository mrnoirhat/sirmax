// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

class RegisterPersonTest {

    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    private final Fakes.InMemoryPeople people = new Fakes.InMemoryPeople();
    private final Fakes.InMemoryIdentifications idents = new Fakes.InMemoryIdentifications();
    private final Fakes.RecordingAuditSink audit = new Fakes.RecordingAuditSink();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(NOW);

    private RegisterPerson register;

    @BeforeEach
    void setUp() {
        register =
                new RegisterPerson(
                        people,
                        idents,
                        ids,
                        clock,
                        new Fakes.DirectUnitOfWork(),
                        new Audit(audit, clock, ids));
    }

    private static AppUser user() {
        return AppUser.create("u1", "op1", "Op", new PasswordHash("FAKE", "h:x"), null, NOW);
    }

    private static Session sessionWith(Permission... perms) {
        return new Session(
                "s1", user(), AccessPolicy.of(EnumSet.copyOf(java.util.List.of(perms))), NOW);
    }

    private RegisterPerson.Command cmd(Session s, String given, String family, IdentificationType t, String num) {
        return new RegisterPerson.Command(
                s,
                given,
                family,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(t),
                Optional.ofNullable(num),
                "test");
    }

    @Test
    void withoutPermissionItIsForbidden() {
        Result<Person> r = register.execute(cmd(sessionWith(Permission.PERSON_READ), "Ana", "Reyes", null, null));
        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("error.forbidden");
        assertThat(people.byId).isEmpty();
    }

    @Test
    void registersAPersonAndRecordsAudit() {
        Result<Person> r =
                register.execute(cmd(sessionWith(Permission.PERSON_WRITE), " Ana  María ", "Reyes", null, null));

        assertThat(r.isOk()).isTrue();
        Person p = r.orElseThrow();
        assertThat(p.fullName()).isEqualTo("Ana María Reyes");
        assertThat(people.byId).containsKey(p.id());
        assertThat(audit.actions()).contains("person.created");
    }

    @Test
    void registersWithAPrimaryIdentification() {
        Result<Person> r =
                register.execute(
                        cmd(
                                sessionWith(Permission.PERSON_WRITE),
                                "Luis",
                                "Cruz",
                                IdentificationType.CEDULA,
                                " 001-1234567-8 "));

        assertThat(r.isOk()).isTrue();
        assertThat(idents.all).hasSize(1);
        assertThat(idents.all.get(0).number()).isEqualTo("001-1234567-8");
        assertThat(idents.all.get(0).primary()).isTrue();
    }

    @Test
    void duplicateIdentificationNumberIsRejected() {
        register.execute(
                cmd(sessionWith(Permission.PERSON_WRITE), "Luis", "Cruz", IdentificationType.CEDULA, "001-1"));
        Result<Person> dup =
                register.execute(
                        cmd(sessionWith(Permission.PERSON_WRITE), "Otro", "Nombre", IdentificationType.CEDULA, "001-1"));

        assertThat(((Result.Err<?>) dup).messageKey()).isEqualTo("person.duplicate_id");
        assertThat(people.byId).hasSize(1);
    }
}
