// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * Backups, their history and the policy that produces them — master prompt §41, §42.
 *
 * <p>{@link org.sirmax.domain.backup.BackupRecord} carries the hash of the archive as written, so
 * validating one is a real integrity test rather than a reassurance.
 * {@link org.sirmax.domain.backup.BackupKind#EMERGENCY} exists because §42 makes the pre-restore
 * copy a step, not a suggestion — it is the only way back if the restore was the mistake. And
 * {@link org.sirmax.domain.backup.BackupSchedule} defaults off-site upload to off: a citizen
 * register must never leave the building without someone deciding it should.
 */
package org.sirmax.domain.backup;
