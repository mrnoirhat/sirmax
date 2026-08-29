// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import org.sirmax.application.catalog.ServiceCatalogTemplates;

/**
 * Supplies the editable seed templates for the service catalog (master prompt §54).
 *
 * <p>The desktop build implements this by reading a bundled Dominican Republic resource; another
 * deployment could read a different country bundle or a downloaded pack.
 */
public interface ServiceCatalogTemplateSource {

    /**
     * @return the template bundle; never {@code null}
     * @throws org.sirmax.shared.SirmaxException if the bundle cannot be read or is malformed
     */
    ServiceCatalogTemplates load();
}
