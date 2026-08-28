// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

import java.util.List;
import java.util.Objects;

/**
 * One entry in the shell's task navigation.
 *
 * @param key destination
 * @param labelKey i18n key for the visible label
 * @param section which group it appears under
 */
public record NavItem(RouteKey key, String labelKey, Section section) {

    public NavItem {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(labelKey, "labelKey");
        Objects.requireNonNull(section, "section");
    }

    public enum Section {
        TASKS("nav.section.tasks"),
        ADMIN("nav.section.admin");

        private final String labelKey;

        Section(String labelKey) {
            this.labelKey = labelKey;
        }

        public String labelKey() {
            return labelKey;
        }
    }

    /** The default task navigation (order matters). */
    public static List<NavItem> defaults() {
        return List.of(
                new NavItem(RouteKey.HOME, "nav.home", Section.TASKS),
                new NavItem(RouteKey.PROCEDURES, "nav.procedures", Section.TASKS),
                new NavItem(RouteKey.BILLING, "nav.billing", Section.TASKS),
                new NavItem(RouteKey.CASH, "nav.cash", Section.TASKS),
                new NavItem(RouteKey.DOCUMENTS, "nav.documents", Section.TASKS),
                new NavItem(RouteKey.CITIZENS, "nav.citizens", Section.TASKS),
                new NavItem(RouteKey.DEPARTMENTS, "nav.departments", Section.ADMIN),
                new NavItem(RouteKey.SETTINGS, "nav.settings", Section.ADMIN),
                new NavItem(RouteKey.REPORTS, "nav.reports", Section.ADMIN));
    }
}
