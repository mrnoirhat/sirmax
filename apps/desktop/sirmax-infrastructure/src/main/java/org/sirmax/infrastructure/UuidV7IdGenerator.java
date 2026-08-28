// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure;

import org.sirmax.application.port.IdGenerator;
import org.sirmax.shared.Ids;

/** {@link IdGenerator} producing time-ordered UUIDv7 strings. */
public final class UuidV7IdGenerator implements IdGenerator {

    @Override
    public String newId() {
        return Ids.newId();
    }
}
