// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An operator account.
 *
 * <p>The domain holds only the {@link PasswordHash}; verifying a plaintext password is the
 * infrastructure's job. Roles are associated separately (see the application layer); this aggregate
 * carries the account's own state.
 */
public final class AppUser {

    private final String id;
    private final String username;
    private String displayName;
    private PasswordHash passwordHash;
    private AppUserStatus status;
    private String departmentId; // nullable
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt; // nullable

    // Lockout state (master prompt §43). Kept on the account rather than in memory so a lock
    // survives a restart — otherwise closing SIRMAX would be the way around it.
    private int failedAttempts;
    private Instant lockedUntil; // nullable

    public AppUser(
            String id,
            String username,
            String displayName,
            PasswordHash passwordHash,
            AppUserStatus status,
            String departmentId,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt) {
        this(
                id,
                username,
                displayName,
                passwordHash,
                status,
                departmentId,
                createdAt,
                updatedAt,
                lastLoginAt,
                0,
                null);
    }

    public AppUser(
            String id,
            String username,
            String displayName,
            PasswordHash passwordHash,
            AppUserStatus status,
            String departmentId,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt,
            int failedAttempts,
            Instant lockedUntil) {
        this.failedAttempts = Math.max(0, failedAttempts);
        this.lockedUntil = lockedUntil;
        this.id = requireText(id, "id");
        this.username = requireText(username, "username");
        this.displayName = requireText(displayName, "displayName");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status");
        this.departmentId = blankToNull(departmentId);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.lastLoginAt = lastLoginAt;
    }

    /** A brand-new active account. */
    public static AppUser create(
            String id,
            String username,
            String displayName,
            PasswordHash passwordHash,
            String departmentId,
            Instant now) {
        return new AppUser(
                id,
                username,
                displayName,
                passwordHash,
                AppUserStatus.ACTIVE,
                departmentId,
                now,
                now,
                null);
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public AppUserStatus status() {
        return status;
    }

    public Optional<String> departmentId() {
        return Optional.ofNullable(departmentId);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Optional<Instant> lockedUntil() {
        return Optional.ofNullable(lockedUntil);
    }

    /** {@code true} while a lockout is still in force at {@code now}. */
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Record a failed sign-in, locking the account once the policy's threshold is reached.
     *
     * @return {@code true} when this attempt is the one that locked it
     */
    public boolean recordFailedSignIn(SecurityPolicy policy, Instant now) {
        failedAttempts++;
        updatedAt = now;
        if (failedAttempts >= policy.maxFailedAttempts()) {
            lockedUntil = now.plus(policy.lockout());
            return true;
        }
        return false;
    }

    /**
     * Clear the lockout — on a successful sign-in, or when an administrator unlocks the account.
     * A lock that expires on its own also runs through here on the next successful attempt.
     */
    public void clearLockout(Instant now) {
        failedAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public Optional<Instant> lastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }

    public boolean canSignIn() {
        return status.canSignIn();
    }

    public void changePassword(PasswordHash newHash, Instant now) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
        touch(now);
    }

    public void rename(String newDisplayName, Instant now) {
        this.displayName = requireText(newDisplayName, "displayName");
        touch(now);
    }

    public void assignDepartment(String departmentId, Instant now) {
        this.departmentId = blankToNull(departmentId);
        touch(now);
    }

    public void changeStatus(AppUserStatus newStatus, Instant now) {
        this.status = Objects.requireNonNull(newStatus, "newStatus");
        touch(now);
    }

    public void recordSignIn(Instant now) {
        clearLockout(now);
        this.lastLoginAt = Objects.requireNonNull(now, "now");
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AppUser u && id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
