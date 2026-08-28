// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.identity.Identification;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.domain.identity.Sex;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Registers a citizen master record, optionally with a primary identification.
 *
 * <p>Requires {@code person.write}. If an identification is supplied and its number already exists,
 * the outcome is {@code person.duplicate_id} rather than a second record (master prompt §23 —
 * duplicate detection; the full fuzzy match lands in Phase 5).
 */
public final class RegisterPerson implements UseCase<RegisterPerson.Command, Person> {

    public record Command(
            Session session,
            String givenNames,
            String familyNames,
            Optional<LocalDate> birthDate,
            Optional<Sex> sex,
            Optional<String> notes,
            Optional<IdentificationType> idType,
            Optional<String> idNumber,
            String source) {}

    private final PersonRepository people;
    private final IdentificationRepository identifications;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public RegisterPerson(
            PersonRepository people,
            IdentificationRepository identifications,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.people = people;
        this.identifications = identifications;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Person> execute(Command c) {
        if (!c.session().can(Permission.PERSON_WRITE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        PersonName name;
        try {
            name = new PersonName(c.givenNames(), c.familyNames());
        } catch (IllegalArgumentException e) {
            return Result.err("INVALID_NAME", "person.invalid_name");
        }

        boolean wantsId = c.idType().isPresent() && c.idNumber().map(s -> !s.isBlank()).orElse(false);
        if (wantsId
                && identifications
                        .findByNumber(c.idType().get(), c.idNumber().get().strip())
                        .isPresent()) {
            return Result.err("DUPLICATE_ID", "person.duplicate_id");
        }

        return Result.ok(unitOfWork.execute(() -> doRegister(c, name, wantsId)));
    }

    private Person doRegister(Command c, PersonName name, boolean wantsId) {
        Instant now = clock.now();
        Person person = Person.create(ids.newId(), name, now);
        person.update(
                name,
                c.birthDate().orElse(null),
                c.sex().orElse(null),
                c.notes().orElse(null),
                now);
        people.save(person);

        if (wantsId) {
            Identification id =
                    Identification.of(
                                    ids.newId(),
                                    person.ref(),
                                    c.idType().get(),
                                    c.idNumber().get().strip(),
                                    now)
                            .asPrimary();
            identifications.save(id);
        }

        audit.record(
                c.session().audit(c.source()), "person.created", "Person", person.id());
        return person;
    }
}
