// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.CashSession;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Opening and closing a cashier's drawer (master prompt §20 — cash drawer, reconciliation).
 *
 * <p>Kept as one class because opening and closing are two halves of one operator ritual and share
 * every collaborator; splitting them would duplicate the wiring for no gain.
 *
 * <p>Closing computes the difference between the expected and counted totals and <b>reports it
 * rather than fixing it</b>. A drawer that is short is information the municipality must have.
 */
public final class ManageCashSession {

    private static final String SEQUENCE = "CAJA";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record OpenCommand(
            Session session, Money openingFloat, Optional<String> departmentId, String source) {}

    public record CloseCommand(
            Session session, Money countedTotal, Optional<String> notes, String source) {}

    /**
     * The close-out figures the cashier signs off on.
     *
     * @param difference counted − expected: positive is over, negative is short, zero balances
     */
    public record Closing(
            CashSession session,
            Money openingFloat,
            Money cashCollected,
            Money cashRefunded,
            Money expected,
            Money counted,
            Money difference) {

        public boolean balances() {
            return difference.isZero();
        }
    }

    private final BillingRepository billing;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public ManageCashSession(
            BillingRepository billing,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.billing = billing;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    /** The cashier's open session, if any — what the UI checks before offering to take cash. */
    public Optional<CashSession> openSessionFor(String userId) {
        return billing.findOpenSessionFor(userId);
    }

    public Result<CashSession> open(OpenCommand c) {
        if (!c.session().can(Permission.CASH_SESSION_OPEN)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.openingFloat().isNegative()) {
            return Result.err("INVALID_FLOAT", "cash.invalid_float");
        }
        // One drawer per cashier: two open sessions would make the close-out count meaningless.
        if (billing.findOpenSessionFor(c.session().user().id()).isPresent()) {
            return Result.err("ALREADY_OPEN", "cash.session_already_open");
        }

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            Instant now = clock.now();
                            String code =
                                    numbering.allocate(
                                            SEQUENCE,
                                            SEQUENCE,
                                            LocalDate.ofInstant(now, LOCAL_ZONE).getYear());
                            CashSession session =
                                    CashSession.open(
                                            ids.newId(),
                                            code,
                                            c.session().user().id(),
                                            c.departmentId().orElse(null),
                                            c.openingFloat(),
                                            now);
                            billing.save(session);
                            audit.record(
                                    c.session().audit(c.source()),
                                    "cash.session_opened",
                                    "CashSession",
                                    session.id(),
                                    null,
                                    code + " " + c.openingFloat(),
                                    null);
                            return session;
                        }));
    }

    public Result<Closing> close(CloseCommand c) {
        if (!c.session().can(Permission.CASH_SESSION_CLOSE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        Optional<CashSession> found = billing.findOpenSessionFor(c.session().user().id());
        if (found.isEmpty()) {
            return Result.err("NO_OPEN_SESSION", "cash.no_open_session");
        }
        CashSession session = found.get();
        if (!c.countedTotal().currency().equals(session.currency())) {
            return Result.err("CURRENCY_MISMATCH", "payment.currency_mismatch");
        }

        return Result.ok(unitOfWork.execute(() -> doClose(c, session)));
    }

    private Closing doClose(CloseCommand c, CashSession session) {
        Instant now = clock.now();
        String currencyCode = session.currency().getCurrencyCode();

        Money collected = billing.cashCollectedIn(session.id(), currencyCode);
        Money refunded = billing.cashRefundedIn(session.id(), currencyCode);
        Money expected = session.expectedCash(collected, refunded);

        Money difference = session.close(c.countedTotal(), expected, c.notes().orElse(null), now);
        billing.save(session);

        audit.record(
                c.session().audit(c.source()),
                "cash.session_closed",
                "CashSession",
                session.id(),
                expected.toString(),
                c.countedTotal().toString(),
                difference.isZero() ? null : "diferencia " + difference);

        return new Closing(
                session,
                session.openingFloat(),
                collected,
                refunded,
                expected,
                c.countedTotal(),
                difference);
    }
}
