// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sirmax.domain.security.AppUser;

/** Persistence for {@link AppUser} accounts and their role assignments. */
public interface UserRepository {

    void save(AppUser user);

    Optional<AppUser> findById(String id);

    /** Case-insensitive username lookup. */
    Optional<AppUser> findByUsername(String username);

    List<AppUser> list();

    long count();

    /** The ids of the roles assigned to a user. */
    Set<String> roleIdsOf(String userId);

    /** Replace a user's role set with exactly {@code roleIds}. */
    void replaceRoles(String userId, Set<String> roleIds);
}
