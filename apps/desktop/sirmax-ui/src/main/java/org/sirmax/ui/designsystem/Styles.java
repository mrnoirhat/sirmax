// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

/**
 * CSS style-class names used by the SIRMAX Design System.
 *
 * <p>Kept as constants so component code and {@code sirmax.css} cannot drift on a typo.
 */
public final class Styles {

    private Styles() {}

    // typography
    public static final String DISPLAY = "sirmax-display";
    public static final String TITLE = "sirmax-title";
    public static final String SUBTITLE = "sirmax-subtitle";
    public static final String BODY = "sirmax-body";
    public static final String MUTED = "sirmax-muted";
    public static final String MONO = "sirmax-mono";

    // buttons
    public static final String PRIMARY = "sirmax-primary";
    public static final String DANGER = "sirmax-danger";
    public static final String GHOST = "sirmax-ghost";

    // shell
    public static final String SHELL = "sirmax-shell";
    public static final String TOPBAR = "sirmax-topbar";
    public static final String BRAND = "sirmax-brand";
    public static final String GLOBAL_SEARCH = "sirmax-global-search";
    public static final String TASKNAV = "sirmax-tasknav";
    public static final String NAV_SECTION = "sirmax-nav-section";
    public static final String NAV_ITEM = "sirmax-navitem";
    public static final String SELECTED = "selected";
    public static final String CONTENT = "sirmax-content";
    public static final String BREADCRUMB = "sirmax-breadcrumb";

    // components
    public static final String CARD = "sirmax-card";
    public static final String TASK_CARD = "sirmax-taskcard";
    public static final String BANNER = "sirmax-banner";
    public static final String BANNER_TITLE = "sirmax-banner-title";
    public static final String TOAST = "sirmax-toast";
    public static final String TOAST_HOST = "sirmax-toast-host";
    public static final String TABLE = "sirmax-table";
    public static final String FIELD = "sirmax-field";
    public static final String FIELD_LABEL = "sirmax-field-label";
    public static final String FIELD_HINT = "sirmax-field-hint";
    public static final String FIELD_ERROR = "sirmax-field-error";
    public static final String INVALID = "invalid";
    public static final String STATE = "sirmax-state";
    public static final String STATE_TITLE = "sirmax-state-title";
    public static final String STATE_DETAIL = "sirmax-state-detail";
    public static final String STATE_TECH = "sirmax-state-tech";
    public static final String SPINNER = "sirmax-spinner";
    public static final String DIALOG = "sirmax-dialog";

    // severities (shared by banner + toast)
    public static final String INFO = "info";
    public static final String SUCCESS = "success";
    public static final String WARNING = "warning";
}
