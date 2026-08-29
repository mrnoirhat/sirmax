// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.RegistryRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.registry.RegisteredDocument;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Presents a document to the municipal register, and later enters it (master prompt §4).
 *
 * <p>Two acts, one class, because they are the two halves of one counter workflow and share every
 * collaborator: {@link #present} takes the document in and assigns its registration number;
 * {@link #register} is the privileged act that gives it a book and folio and legal standing.
 *
 * <p>Presenting needs {@code document.register}; entering it into the register needs
 * {@code document.certify}. That separation is the whole reason the register is trustworthy — the
 * clerk who receives a deed is not the officer who books it.
 */
public final class RegisterDocument
        implements UseCase<RegisterDocument.PresentCommand, RegisteredDocument> {

    private static final String SEQUENCE = "REG";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    /**
     * @param parties who the document names, and in what role — a deed with no grantor is not a deed
     */
    public record PresentCommand(
            Session session,
            String documentType,
            String title,
            Optional<LocalDate> documentDate,
            List<PartyRole> parties,
            Optional<String> procedureId,
            Optional<String> relatedAssetId,
            String source) {}

    public record PartyRole(PartyRef party, String role) {}

    public record RegisterCommand(
            Session session,
            String documentId,
            String book,
            Optional<String> volume,
            String folio,
            String source) {}

    private final RegistryRepository registry;
    private final ProcedureRepository procedures;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public RegisterDocument(
            RegistryRepository registry,
            ProcedureRepository procedures,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.registry = registry;
        this.procedures = procedures;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<RegisteredDocument> execute(PresentCommand c) {
        return present(c);
    }

    /** Take a document in at the counter and give it its registration number. */
    public Result<RegisteredDocument> present(PresentCommand c) {
        if (!c.session().can(Permission.DOCUMENT_REGISTER)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.title() == null || c.title().isBlank()) {
            return Result.err("TITLE_REQUIRED", "registry.title_required");
        }
        if (c.documentType() == null || c.documentType().isBlank()) {
            return Result.err("TYPE_REQUIRED", "registry.type_required");
        }
        if (c.parties() == null || c.parties().isEmpty()) {
            return Result.err("PARTIES_REQUIRED", "registry.parties_required");
        }

        return Result.ok(unitOfWork.execute(() -> doPresent(c)));
    }

    private RegisteredDocument doPresent(PresentCommand c) {
        Instant now = clock.now();
        String number =
                numbering.allocate(
                        SEQUENCE, SEQUENCE, LocalDate.ofInstant(now, LOCAL_ZONE).getYear());

        RegisteredDocument document =
                RegisteredDocument.presented(
                        ids.newId(),
                        number,
                        c.documentType(),
                        c.title(),
                        c.procedureId().orElse(null),
                        now);
        c.documentDate()
                .ifPresent(
                        date ->
                                document.updateDetails(
                                        c.documentType(), c.title(), date, now));
        c.relatedAssetId().ifPresent(assetId -> document.relateToAsset(assetId, now));
        for (PartyRole role : c.parties()) {
            document.addParty(
                    new RegisteredDocument.Party(
                            ids.newId(), document.id(), role.party(), role.role()));
        }
        registry.save(document);

        c.procedureId()
                .ifPresent(
                        procedureId ->
                                procedures.appendEvent(
                                        ProcedureEvent.of(
                                                ids.newId(),
                                                procedureId,
                                                ProcedureEventKind.DOCUMENT_ISSUED,
                                                c.session().user().id(),
                                                number + " · " + c.title(),
                                                now)));

        audit.record(
                c.session().audit(c.source()),
                "registry.document_presented",
                "RegisteredDocument",
                document.id(),
                null,
                number,
                null);
        return document;
    }

    /**
     * Enter a presented document into the register at a book and folio. Needs
     * {@code document.certify}: this is the act that gives the entry legal standing, and from here
     * corrections are marginal annotations only.
     */
    public Result<RegisteredDocument> register(RegisterCommand c) {
        if (!c.session().can(Permission.DOCUMENT_CERTIFY)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.book() == null || c.book().isBlank() || c.folio() == null || c.folio().isBlank()) {
            return Result.err("BOOK_FOLIO_REQUIRED", "registry.book_folio_required");
        }

        Optional<RegisteredDocument> found = registry.findDocumentById(c.documentId());
        if (found.isEmpty()) {
            return Result.err("DOCUMENT_NOT_FOUND", "registry.not_found");
        }
        RegisteredDocument document = found.get();
        if (document.status().isFrozen()) {
            return Result.err("ALREADY_REGISTERED", "registry.already_registered");
        }
        if (document.status() == RegisteredDocument.Status.REJECTED) {
            return Result.err("REJECTED", "registry.rejected");
        }

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            Instant now = clock.now();
                            document.register(
                                    c.book(), c.volume().orElse(null), c.folio(), now);
                            registry.save(document);
                            audit.record(
                                    c.session().audit(c.source()),
                                    "registry.document_registered",
                                    "RegisteredDocument",
                                    document.id(),
                                    null,
                                    "libro " + c.book() + " folio " + c.folio(),
                                    null);
                            return document;
                        }));
    }
}
