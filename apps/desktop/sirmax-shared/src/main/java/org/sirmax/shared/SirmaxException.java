// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

/**
 * Base type for unexpected faults in SIRMAX (programming errors, infrastructure failures).
 *
 * <p>Expected, user-facing outcomes use {@link Result} instead of exceptions.
 */
public class SirmaxException extends RuntimeException {

    public SirmaxException(String message) {
        super(message);
    }

    public SirmaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
