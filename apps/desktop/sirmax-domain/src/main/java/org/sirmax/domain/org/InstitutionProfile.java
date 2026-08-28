// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.org;

import java.util.Objects;
import java.util.Optional;

/**
 * Branding and contact identity for invoices and official documents (master prompt §59C).
 *
 * <p>Every field is optional except the owning {@link OrganizationUnit}: an install can start with a
 * bare profile and fill it in. Colours are stored as strings (e.g. {@code "#1f5fa6"}); rendering is
 * expected to keep documents readable in black &amp; white and never use colour as the only signal.
 * The value is immutable — "editing" produces a new instance via {@link #with}.
 */
public record InstitutionProfile(
        String organizationUnitId,
        Optional<String> legalIdentifier,
        Optional<String> address,
        Optional<String> phone,
        Optional<String> email,
        Optional<String> website,
        Optional<String> logoPath,
        Optional<String> secondaryLogoPath,
        Optional<String> colorPrimary,
        Optional<String> colorSecondary,
        Optional<String> colorAccent,
        Optional<String> colorText,
        Optional<String> colorBackground,
        Optional<String> invoiceFooter,
        Optional<String> documentHeader) {

    public InstitutionProfile {
        Objects.requireNonNull(organizationUnitId, "organizationUnitId");
        if (organizationUnitId.isBlank()) {
            throw new IllegalArgumentException("organizationUnitId must not be blank");
        }
        legalIdentifier = norm(legalIdentifier);
        address = norm(address);
        phone = norm(phone);
        email = norm(email);
        website = norm(website);
        logoPath = norm(logoPath);
        secondaryLogoPath = norm(secondaryLogoPath);
        colorPrimary = norm(colorPrimary);
        colorSecondary = norm(colorSecondary);
        colorAccent = norm(colorAccent);
        colorText = norm(colorText);
        colorBackground = norm(colorBackground);
        invoiceFooter = norm(invoiceFooter);
        documentHeader = norm(documentHeader);
    }

    /** An empty profile for a newly created organization. */
    public static InstitutionProfile empty(String organizationUnitId) {
        Optional<String> n = Optional.empty();
        return new InstitutionProfile(
                organizationUnitId, n, n, n, n, n, n, n, n, n, n, n, n, n, n);
    }

    /** A builder-style copy that overrides selected fields (each a nullable value). */
    public InstitutionProfile with(Overrides o) {
        return new InstitutionProfile(
                organizationUnitId,
                pick(o.legalIdentifier, legalIdentifier),
                pick(o.address, address),
                pick(o.phone, phone),
                pick(o.email, email),
                pick(o.website, website),
                pick(o.logoPath, logoPath),
                pick(o.secondaryLogoPath, secondaryLogoPath),
                pick(o.colorPrimary, colorPrimary),
                pick(o.colorSecondary, colorSecondary),
                pick(o.colorAccent, colorAccent),
                pick(o.colorText, colorText),
                pick(o.colorBackground, colorBackground),
                pick(o.invoiceFooter, invoiceFooter),
                pick(o.documentHeader, documentHeader));
    }

    /** Nullable override fields; {@code null} means "leave unchanged", {@code ""} means "clear". */
    public static final class Overrides {
        public String legalIdentifier;
        public String address;
        public String phone;
        public String email;
        public String website;
        public String logoPath;
        public String secondaryLogoPath;
        public String colorPrimary;
        public String colorSecondary;
        public String colorAccent;
        public String colorText;
        public String colorBackground;
        public String invoiceFooter;
        public String documentHeader;
    }

    private static Optional<String> pick(String override, Optional<String> current) {
        if (override == null) {
            return current;
        }
        return override.isBlank() ? Optional.empty() : Optional.of(override.strip());
    }

    private static Optional<String> norm(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
