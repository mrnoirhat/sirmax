// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.security.Role;

/** Persistence for {@link Role}s and their permission grants. */
public interface RoleRepository {

    void save(Role role);

    Optional<Role> findById(String id);

    /** Case-insensitive role-name lookup (names are unique). */
    Optional<Role> findByName(String name);

    List<Role> list();

    List<Role> findAllById(Collection<String> ids);

    /** The roles assigned to a user, with their permissions populated. */
    List<Role> rolesOf(String userId);
}
