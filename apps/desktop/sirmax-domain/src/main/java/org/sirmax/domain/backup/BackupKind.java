// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.backup;

/**
 * Why a backup exists (master prompt §41, §42).
 *
 * <p>{@link #EMERGENCY} is a first-class kind rather than a note on a manual backup because §42
 * makes it a step: the state being replaced is always captured before a restore overwrites it. That
 * copy is the only way back if the restore turns out to be the mistake.
 */
public enum BackupKind {
    MANUAL,
    SCHEDULED,
    /** Taken automatically of the current state immediately before a restore (§42 step 1). */
    EMERGENCY,
    /** Taken before applying schema migrations, so a bad migration is recoverable. */
    PRE_MIGRATION;

    /**
     * {@code true} for copies the retention sweep may delete. Emergency and pre-migration backups
     * are kept: they exist precisely for the moment something went wrong, which is exactly when a
     * retention rule would otherwise have thrown them away.
     */
    public boolean isRoutine() {
        return this == MANUAL || this == SCHEDULED;
    }
}
