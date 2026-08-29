// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.Optional;

/**
 * An off-site destination for backups — Google Drive today (master prompt §41).
 *
 * <p>§41 says the user's own Google account owns the destination, and that data must never be
 * uploaded silently. So this port has no notion of a SIRMAX-owned bucket: it uploads to a folder the
 * municipality named, using credentials the municipality granted, and only when the schedule says
 * so.
 *
 * <p>{@link #isConfigured()} is the gate. A municipality that has not connected an account has no
 * off-site copies at all, which is a supported way to run SIRMAX, not a degraded one.
 */
public interface CloudBackupTarget {

    /** Provider identifier stored on the backup record, e.g. {@code GOOGLE_DRIVE}. */
    String provider();

    /** {@code true} once an account has been connected and a folder chosen. */
    boolean isConfigured();

    /**
     * Upload an archive.
     *
     * @param folderId the folder the municipality chose
     * @return the provider's file id, for later verification or download
     * @throws org.sirmax.shared.SirmaxException if the upload fails; the local copy is unaffected
     */
    String upload(String storagePath, String fileName, String folderId);

    /** Whether a previously uploaded file is still present at the destination. */
    boolean exists(String fileId);

    /** The account currently connected, for the settings screen to show. */
    Optional<String> connectedAccount();
}
