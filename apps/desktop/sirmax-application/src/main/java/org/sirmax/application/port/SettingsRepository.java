// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.Map;
import java.util.Optional;

/**
 * Key/value application settings, each stored as a JSON string with a data classification
 * (master prompt §48, §52).
 */
public interface SettingsRepository {

    enum Classification {
        PUBLIC,
        INTERNAL,
        CONFIDENTIAL,
        RESTRICTED
    }

    Optional<String> get(String key);

    void put(String key, String valueJson, Classification classification);

    void remove(String key);

    /** All settings as key → JSON value. */
    Map<String, String> all();
}
