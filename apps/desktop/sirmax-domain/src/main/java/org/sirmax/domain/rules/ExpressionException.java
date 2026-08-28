// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.rules;

/** Thrown when a configuration condition expression is malformed. */
public final class ExpressionException extends RuntimeException {

    public ExpressionException(String message) {
        super(message);
    }
}
