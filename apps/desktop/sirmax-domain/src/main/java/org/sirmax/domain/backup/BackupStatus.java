// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.backup;

/** Where a backup stands in the §41 pipeline. */
public enum BackupStatus {
    /** Written to disk, hash recorded, not yet re-read. */
    CREATED,
    /** Re-read and the hash matched — the archive is what SIRMAX wrote. */
    VALIDATED,
    /** Also copied off-site (§41 optional Drive step). */
    UPLOADED,
    /** The backup could not be completed; the file, if any, is not trustworthy. */
    FAILED,
    /** Validation found a hash mismatch: disk rot, a partial write, or tampering. */
    CORRUPT,
    /**
     * The archive was deleted by the retention policy. The record stays: knowing a backup existed on
     * a given night, and what it contained, outlives the bytes by years — and if it was uploaded,
     * the remote copy is still there.
     */
    PRUNED;

    /**
     * {@code true} when this backup may be restored from. A CREATED backup is deliberately excluded:
     * §42 step 2 requires validating the target first, and "probably fine" is not a standard to
     * apply to the act that overwrites the municipality's database.
     */
    public boolean isRestorable() {
        return this == VALIDATED || this == UPLOADED;
    }
}
