// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.theme;

/** The visual themes SIRMAX ships. Tokens for both live in {@code theme/sirmax.css}. */
public enum Theme {
    LIGHT("theme.light"),
    DARK("theme.dark");

    private final String labelKey;

    Theme(String labelKey) {
        this.labelKey = labelKey;
    }

    public String labelKey() {
        return labelKey;
    }

    public Theme toggled() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
