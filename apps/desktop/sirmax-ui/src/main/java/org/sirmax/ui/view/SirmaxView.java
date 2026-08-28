// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import javafx.scene.Parent;
import org.sirmax.ui.nav.RouteKey;

/** A top-level screen mounted in the shell content area. */
public interface SirmaxView {

    /** Which navigation destination this view serves. */
    RouteKey route();

    /** i18n key for the breadcrumb / heading shown by the shell. */
    String titleKey();

    /** The view's root node. Implementations build it once. */
    Parent node();
}
