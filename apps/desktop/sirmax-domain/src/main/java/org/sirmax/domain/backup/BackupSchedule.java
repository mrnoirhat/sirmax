// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.backup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * The installation's backup policy (master prompt §41 — automatic scheduling).
 *
 * <p>Off-site upload defaults to <b>off</b>. §41 is explicit that sensitive data must never leave
 * for an external service silently, and a municipality's citizen register is about as sensitive as
 * local data gets; turning it on has to be a decision someone made.
 */
public final class BackupSchedule {

    private boolean enabled;
    private Frequency frequency;
    private int hourOfDay;
    private int keepCopies;
    private boolean encrypt;
    private boolean uploadToDrive;
    private String driveFolderId; // nullable
    private Instant lastRunAt; // nullable
    private Instant updatedAt;

    public enum Frequency {
        DAILY,
        WEEKLY,
        MONTHLY;

        /** How many days should pass between runs. */
        public int intervalDays() {
            return switch (this) {
                case DAILY -> 1;
                case WEEKLY -> 7;
                case MONTHLY -> 30;
            };
        }
    }

    public BackupSchedule(
            boolean enabled,
            Frequency frequency,
            int hourOfDay,
            int keepCopies,
            boolean encrypt,
            boolean uploadToDrive,
            String driveFolderId,
            Instant lastRunAt,
            Instant updatedAt) {
        this.enabled = enabled;
        this.frequency = Objects.requireNonNull(frequency, "frequency");
        this.hourOfDay = requireHour(hourOfDay);
        this.keepCopies = requireKeep(keepCopies);
        this.encrypt = encrypt;
        this.uploadToDrive = uploadToDrive;
        this.driveFolderId = blankToNull(driveFolderId);
        this.lastRunAt = lastRunAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** The safe default: a nightly encrypted local copy, nothing leaving the building. */
    public static BackupSchedule defaults(Instant now) {
        return new BackupSchedule(true, Frequency.DAILY, 20, 30, true, false, null, null, now);
    }

    public boolean enabled() {
        return enabled;
    }

    public Frequency frequency() {
        return frequency;
    }

    public int hourOfDay() {
        return hourOfDay;
    }

    public int keepCopies() {
        return keepCopies;
    }

    public boolean encrypt() {
        return encrypt;
    }

    public boolean uploadToDrive() {
        return uploadToDrive;
    }

    public Optional<String> driveFolderId() {
        return Optional.ofNullable(driveFolderId);
    }

    public Optional<Instant> lastRunAt() {
        return Optional.ofNullable(lastRunAt);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * {@code true} when a scheduled backup is due.
     *
     * <p>Deliberately forgiving about the hour: an office PC switched off at 20:00 should back up
     * when it is next switched on, not skip the day. The comparison is on whole days since the last
     * run, so a machine used only in the mornings still gets its nightly copy.
     */
    public boolean isDue(Instant now, ZoneId zone) {
        if (!enabled) {
            return false;
        }
        if (lastRunAt == null) {
            return true;
        }
        LocalDate lastDay = LocalDateTime.ofInstant(lastRunAt, zone).toLocalDate();
        LocalDateTime current = LocalDateTime.ofInstant(now, zone);
        long daysSince = ChronoUnit.DAYS.between(lastDay, current.toLocalDate());
        if (daysSince > frequency.intervalDays()) {
            return true;
        }
        return daysSince == frequency.intervalDays() && current.getHour() >= hourOfDay;
    }

    public void configure(
            boolean newEnabled,
            Frequency newFrequency,
            int newHourOfDay,
            int newKeepCopies,
            boolean newEncrypt,
            Instant now) {
        this.enabled = newEnabled;
        this.frequency = Objects.requireNonNull(newFrequency, "frequency");
        this.hourOfDay = requireHour(newHourOfDay);
        this.keepCopies = requireKeep(newKeepCopies);
        this.encrypt = newEncrypt;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** Turning on off-site copies requires naming the folder they go to — never a default one. */
    public void enableDriveUpload(String folderId, Instant now) {
        String folder = blankToNull(folderId);
        if (folder == null) {
            throw new IllegalArgumentException(
                    "Uploading to Drive requires the folder the municipality chose");
        }
        this.uploadToDrive = true;
        this.driveFolderId = folder;
        this.updatedAt = now;
    }

    public void disableDriveUpload(Instant now) {
        this.uploadToDrive = false;
        this.updatedAt = now;
    }

    public void recordRun(Instant now) {
        this.lastRunAt = now;
        this.updatedAt = now;
    }

    private static int requireHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hourOfDay must be between 0 and 23");
        }
        return hour;
    }

    private static int requireKeep(int keep) {
        if (keep < 1) {
            throw new IllegalArgumentException("keepCopies must be at least 1");
        }
        return keep;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }
}
