// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.security.Session;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;
import org.sirmax.shared.text.Normalization;

/**
 * Finds people who may already be the citizen at the counter, before a second record is created
 * (master prompt §23 — "avoid storing the same citizen separately in every procedure").
 *
 * <p>Two signals, ranked:
 *
 * <ul>
 *   <li>an <b>exact identification match</b> is conclusive — same cédula, same person;
 *   <li>a <b>name similarity</b> above the threshold is a suggestion the operator confirms, boosted
 *       when the birth date also matches.
 * </ul>
 *
 * <p>Name comparison normalizes case and Spanish accents and uses a token-overlap score, which
 * survives the ordering and spelling variation real registries contain ("José Luis Pérez Gómez" vs
 * "Jose Perez"). This never merges anything on its own: the operator decides.
 */
public final class FindDuplicatePeople
        implements UseCase<FindDuplicatePeople.Command, List<FindDuplicatePeople.Candidate>> {

    /** Below this token-overlap score two names are not offered as the same person. */
    private static final double NAME_THRESHOLD = 0.55;

    private static final int SEARCH_WIDTH = 50;

    public record Command(
            Session session,
            String givenNames,
            String familyNames,
            Optional<LocalDate> birthDate,
            Optional<IdentificationType> idType,
            Optional<String> idNumber) {}

    /**
     * @param score 1.0 for an identification match, otherwise the name similarity
     * @param reason i18n key explaining why this record surfaced
     */
    public record Candidate(Person person, double score, String reason)
            implements Comparable<Candidate> {

        public boolean isConclusive() {
            return score >= 1.0;
        }

        @Override
        public int compareTo(Candidate other) {
            return Double.compare(other.score, score);
        }
    }

    private final PersonRepository people;
    private final IdentificationRepository identifications;

    public FindDuplicatePeople(PersonRepository people, IdentificationRepository identifications) {
        this.people = people;
        this.identifications = identifications;
    }

    @Override
    public Result<List<Candidate>> execute(Command c) {
        if (!c.session().can(Permission.PERSON_READ)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        List<Candidate> candidates = new ArrayList<>();

        if (c.idType().isPresent() && c.idNumber().filter(s -> !s.isBlank()).isPresent()) {
            identifications
                    .findByNumber(c.idType().get(), c.idNumber().get().strip())
                    .flatMap(id -> people.findById(id.owner().id()))
                    .ifPresent(p -> candidates.add(new Candidate(p, 1.0, "person.duplicate_id")));
        }

        String full = (c.givenNames() + " " + c.familyNames()).strip();
        if (!full.isBlank()) {
            String familyProbe = c.familyNames().isBlank() ? full : c.familyNames();
            for (Person other : people.search(firstToken(familyProbe), SEARCH_WIDTH, 0)) {
                if (candidates.stream().anyMatch(x -> x.person().id().equals(other.id()))) {
                    continue;
                }
                double score = Normalization.similarity(full, other.fullName());
                boolean sameBirthDate =
                        c.birthDate().isPresent() && c.birthDate().equals(other.birthDate());
                if (sameBirthDate) {
                    score = Math.min(0.99, score + 0.2);
                }
                if (score >= NAME_THRESHOLD) {
                    candidates.add(
                            new Candidate(
                                    other,
                                    score,
                                    sameBirthDate
                                            ? "person.similar_name_and_birthdate"
                                            : "person.similar_name"));
                }
            }
        }

        candidates.sort(Comparator.naturalOrder());
        return Result.ok(List.copyOf(candidates));
    }

    private static String firstToken(String value) {
        String[] parts = value.strip().split("\\s+");
        return parts.length == 0 ? value : parts[0];
    }
}
