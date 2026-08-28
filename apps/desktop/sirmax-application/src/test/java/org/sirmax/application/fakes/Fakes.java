// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.fakes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.sirmax.application.port.AuditSink;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.port.UserRepository;
import org.sirmax.domain.audit.AuditEvent;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.Identification;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Role;

/** In-memory test doubles for the application ports. */
public final class Fakes {

    private Fakes() {}

    public static final class FixedClock implements Clock {
        private Instant now;

        public FixedClock(Instant now) {
            this.now = now;
        }

        public void set(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }
    }

    public static final class SeqIds implements IdGenerator {
        private final AtomicInteger n = new AtomicInteger();

        @Override
        public String newId() {
            return String.format("id-%04d", n.incrementAndGet());
        }
    }

    /** Runs work directly — no real transaction. */
    public static final class DirectUnitOfWork implements UnitOfWork {
        @Override
        public <T> T execute(Supplier<T> work) {
            return work.get();
        }
    }

    /** Reversible "hash" for deterministic tests: hash = "h:" + plaintext. */
    public static final class ReversibleHasher implements PasswordHasher {
        @Override
        public PasswordHash hash(char[] plaintext) {
            return new PasswordHash("FAKE", "h:" + new String(plaintext));
        }

        @Override
        public boolean verify(char[] plaintext, PasswordHash stored) {
            return stored.value().equals("h:" + new String(plaintext));
        }
    }

    public static final class RecordingAuditSink implements AuditSink {
        public final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }

        public List<String> actions() {
            return events.stream().map(AuditEvent::action).toList();
        }
    }

    public static final class InMemoryUsers implements UserRepository {
        private final Map<String, AppUser> byId = new LinkedHashMap<>();
        private final Map<String, Set<String>> roles = new HashMap<>();

        @Override
        public void save(AppUser user) {
            byId.put(user.id(), user);
        }

        @Override
        public Optional<AppUser> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<AppUser> findByUsername(String username) {
            return byId.values().stream()
                    .filter(u -> u.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public List<AppUser> list() {
            return List.copyOf(byId.values());
        }

        @Override
        public long count() {
            return byId.size();
        }

        @Override
        public Set<String> roleIdsOf(String userId) {
            return roles.getOrDefault(userId, Set.of());
        }

        @Override
        public void replaceRoles(String userId, Set<String> roleIds) {
            roles.put(userId, Set.copyOf(roleIds));
        }
    }

    public static final class InMemoryRoles implements RoleRepository {
        private final Map<String, Role> byId = new LinkedHashMap<>();
        private final InMemoryUsers users;

        public InMemoryRoles(InMemoryUsers users) {
            this.users = users;
        }

        public InMemoryRoles add(Role role) {
            byId.put(role.id(), role);
            return this;
        }

        @Override
        public void save(Role role) {
            byId.put(role.id(), role);
        }

        @Override
        public Optional<Role> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Role> findByName(String name) {
            return byId.values().stream()
                    .filter(r -> r.name().equalsIgnoreCase(name))
                    .findFirst();
        }

        @Override
        public List<Role> list() {
            return List.copyOf(byId.values());
        }

        @Override
        public List<Role> findAllById(Collection<String> ids) {
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<Role> rolesOf(String userId) {
            return users.roleIdsOf(userId).stream()
                    .map(byId::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }

    public static final class InMemoryOrganizations implements OrganizationRepository {
        public final Map<String, OrganizationUnit> units = new LinkedHashMap<>();
        public final Map<String, InstitutionProfile> profiles = new LinkedHashMap<>();
        public final Map<String, Department> departments = new LinkedHashMap<>();

        @Override
        public void save(OrganizationUnit unit) {
            units.put(unit.id(), unit);
        }

        @Override
        public Optional<OrganizationUnit> findById(String id) {
            return Optional.ofNullable(units.get(id));
        }

        @Override
        public Optional<OrganizationUnit> findActive() {
            return units.values().stream().findFirst();
        }

        @Override
        public void saveProfile(InstitutionProfile profile) {
            profiles.put(profile.organizationUnitId(), profile);
        }

        @Override
        public Optional<InstitutionProfile> findProfile(String organizationUnitId) {
            return Optional.ofNullable(profiles.get(organizationUnitId));
        }

        @Override
        public void save(Department department) {
            departments.put(department.id(), department);
        }

        @Override
        public Optional<Department> findDepartmentById(String id) {
            return Optional.ofNullable(departments.get(id));
        }

        @Override
        public Optional<Department> findDepartmentByCode(String organizationUnitId, String code) {
            return departments.values().stream()
                    .filter(
                            d ->
                                    d.organizationUnitId().equals(organizationUnitId)
                                            && d.code().equalsIgnoreCase(code))
                    .findFirst();
        }

        @Override
        public List<Department> listActiveDepartments(String organizationUnitId) {
            return departments.values().stream()
                    .filter(d -> d.organizationUnitId().equals(organizationUnitId) && d.isActive())
                    .toList();
        }
    }

    public static final class InMemoryPeople implements PersonRepository {
        public final Map<String, Person> byId = new LinkedHashMap<>();

        @Override
        public void save(Person person) {
            byId.put(person.id(), person);
        }

        @Override
        public Optional<Person> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Person> search(String query, int limit, int offset) {
            String q = query == null ? "" : query.toLowerCase();
            return byId.values().stream()
                    .filter(p -> q.isBlank() || p.fullName().toLowerCase().contains(q))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countSearch(String query) {
            return search(query, Integer.MAX_VALUE, 0).size();
        }
    }

    public static final class InMemoryIdentifications implements IdentificationRepository {
        public final List<Identification> all = new ArrayList<>();

        @Override
        public void save(Identification identification) {
            all.add(identification);
        }

        @Override
        public List<Identification> forOwner(PartyRef owner) {
            return all.stream().filter(i -> i.owner().equals(owner)).toList();
        }

        @Override
        public Optional<Identification> findByNumber(IdentificationType type, String number) {
            return all.stream()
                    .filter(i -> i.type() == type && i.number().equalsIgnoreCase(number))
                    .findFirst();
        }
    }

    public static final class InMemoryServiceCatalog
            implements org.sirmax.application.port.ServiceCatalogRepository {
        public final Map<String, org.sirmax.domain.service.ServiceCategory> categories =
                new LinkedHashMap<>();
        public final Map<String, org.sirmax.domain.service.ServiceDefinition> definitions =
                new LinkedHashMap<>();
        public final Map<String, org.sirmax.domain.service.ServiceDefinitionVersion> versions =
                new LinkedHashMap<>();

        @Override
        public void saveCategory(org.sirmax.domain.service.ServiceCategory category) {
            categories.put(category.id(), category);
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceCategory> findCategoryById(String id) {
            return Optional.ofNullable(categories.get(id));
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceCategory> findCategoryByCode(String code) {
            return categories.values().stream()
                    .filter(c -> c.code().equalsIgnoreCase(code))
                    .findFirst();
        }

        @Override
        public List<org.sirmax.domain.service.ServiceCategory> listActiveCategories() {
            return categories.values().stream().filter(c -> c.isActive()).toList();
        }

        @Override
        public void saveDefinition(org.sirmax.domain.service.ServiceDefinition definition) {
            definitions.put(definition.id(), definition);
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceDefinition> findDefinitionById(String id) {
            return Optional.ofNullable(definitions.get(id));
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceDefinition> findDefinitionByCode(
                String code) {
            return definitions.values().stream()
                    .filter(d -> d.code().equalsIgnoreCase(code))
                    .findFirst();
        }

        @Override
        public List<org.sirmax.domain.service.ServiceDefinition> listDefinitions(
                boolean includeArchived) {
            return definitions.values().stream()
                    .filter(
                            d ->
                                    includeArchived
                                            || d.archiveStatus()
                                                    == org.sirmax.domain.common.ArchiveStatus.ACTIVE)
                    .toList();
        }

        @Override
        public void saveVersion(org.sirmax.domain.service.ServiceDefinitionVersion version) {
            versions.put(version.id(), version);
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceDefinitionVersion> findVersionById(
                String id) {
            return Optional.ofNullable(versions.get(id));
        }

        @Override
        public List<org.sirmax.domain.service.ServiceDefinitionVersion> listVersions(
                String serviceDefinitionId) {
            return versions.values().stream()
                    .filter(v -> v.serviceDefinitionId().equals(serviceDefinitionId))
                    .sorted(java.util.Comparator.comparingInt(v -> v.versionNumber()))
                    .toList();
        }

        @Override
        public Optional<org.sirmax.domain.service.ServiceDefinitionVersion> findActiveVersion(
                String serviceDefinitionId) {
            return versions.values().stream()
                    .filter(
                            v ->
                                    v.serviceDefinitionId().equals(serviceDefinitionId)
                                            && v.status()
                                                    == org.sirmax.domain.service.ServiceStatus.ACTIVE)
                    .findFirst();
        }

        @Override
        public int nextVersionNumber(String serviceDefinitionId) {
            return listVersions(serviceDefinitionId).stream()
                            .mapToInt(v -> v.versionNumber())
                            .max()
                            .orElse(0)
                    + 1;
        }
    }
}
