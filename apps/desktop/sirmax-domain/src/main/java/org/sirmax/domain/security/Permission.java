// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * The catalog of atomic permissions (master prompt §43). The {@link #key()} of each value matches a
 * row seeded by {@code V0002__core_schema.sql}; the two must stay in sync.
 */
public enum Permission {
    PERSON_READ("person.read"),
    PERSON_WRITE("person.write"),
    DEPARTMENT_MANAGE("department.manage"),
    USER_MANAGE("user.manage"),
    ROLE_MANAGE("role.manage"),
    SERVICE_READ("service.read"),
    SERVICE_CONFIGURE("service.configure"),
    PROCEDURE_READ("procedure.read"),
    PROCEDURE_WORK("procedure.work"),
    PROCEDURE_DECIDE("procedure.decide"),
    FEE_OVERRIDE("fee.override"),
    INVOICE_ISSUE("invoice.issue"),
    INVOICE_VOID("invoice.void"),
    INVOICE_REPRINT("invoice.reprint"),
    PAYMENT_REGISTER("payment.register"),
    PAYMENT_REFUND("payment.refund"),
    CASH_SESSION_OPEN("cash.session.open"),
    CASH_SESSION_CLOSE("cash.session.close"),
    DOCUMENT_REGISTER("document.register"),
    DOCUMENT_CERTIFY("document.certify"),
    CONFIG_MANAGE("config.manage"),
    AUDIT_READ("audit.read"),
    BACKUP_RUN("backup.run"),
    BACKUP_RESTORE("backup.restore"),
    REPORT_VIEW("report.view");

    private final String key;

    Permission(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<Permission> fromKey(String key) {
        return Arrays.stream(values()).filter(p -> p.key.equals(key)).findFirst();
    }
}
