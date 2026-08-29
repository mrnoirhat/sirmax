// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.shared.Money;
import org.sirmax.shared.SirmaxException;

/**
 * Serializes a {@link DocumentSnapshot} to and from {@code issued_document.snapshot_json}.
 *
 * <p>Money is written as {@code {minor, currency}} — the integer and its ISO code, never a decimal
 * string. A snapshot read back years from now must produce the identical {@link Money}, and parsing
 * "1500.00" back into minor units would put a rounding rule between the document and its own past.
 *
 * <p>Explicit node building, like {@link ServiceJson}: the domain model carries no Jackson
 * annotations, and Jackson stays inside infrastructure (ADR 0005).
 */
final class DocumentJson {

    private static final ObjectMapper M = new ObjectMapper();

    String toJson(DocumentSnapshot s) {
        ObjectNode root = M.createObjectNode();
        root.put("kind", s.kind().name());
        root.put("documentNumber", s.documentNumber());
        root.put("issuedAt", s.issuedAt().toString());
        root.put("verificationCode", s.verificationCode());
        s.reference().ifPresent(v -> root.put("reference", v));
        s.issuedByName().ifPresent(v -> root.put("issuedByName", v));
        s.footerNote().ifPresent(v -> root.put("footerNote", v));

        ObjectNode institution = root.putObject("institution");
        institution.put("name", s.institution().name());
        putOpt(institution, "department", s.institution().department());
        putOpt(institution, "municipality", s.institution().municipality());
        putOpt(institution, "legalIdentifier", s.institution().legalIdentifier());
        putOpt(institution, "address", s.institution().address());
        putOpt(institution, "phone", s.institution().phone());
        putOpt(institution, "email", s.institution().email());
        putOpt(institution, "website", s.institution().website());
        putOpt(institution, "logoPath", s.institution().logoPath());

        ObjectNode customer = root.putObject("customer");
        customer.put("name", s.customer().name());
        putOpt(customer, "identificationType", s.customer().identificationType());
        putOpt(customer, "identificationNumber", s.customer().identificationNumber());
        putOpt(customer, "address", s.customer().address());
        putOpt(customer, "phone", s.customer().phone());

        ArrayNode lines = root.putArray("lines");
        for (DocumentSnapshot.Line line : s.lines()) {
            ObjectNode node = lines.addObject();
            node.put("concept", line.concept());
            putOpt(node, "description", line.description());
            node.put("quantity", line.quantity());
            putOpt(node, "unit", line.unit());
            putMoney(node, "unitPrice", line.unitPrice());
            putMoney(node, "discount", line.discount());
            putMoney(node, "surcharge", line.surcharge());
            putMoney(node, "lineTotal", line.lineTotal());
        }

        ObjectNode totals = root.putObject("totals");
        putMoney(totals, "subtotal", s.totals().subtotal());
        putMoney(totals, "discount", s.totals().discount());
        putMoney(totals, "surcharge", s.totals().surcharge());
        putMoney(totals, "total", s.totals().total());
        putMoney(totals, "paid", s.totals().paid());
        putMoney(totals, "balance", s.totals().balance());

        s.payment()
                .ifPresent(
                        p -> {
                            ObjectNode payment = root.putObject("payment");
                            payment.put("method", p.method());
                            putMoney(payment, "amount", p.amount());
                            p.tendered().ifPresent(v -> putMoney(payment, "tendered", v));
                            p.change().ifPresent(v -> putMoney(payment, "change", v));
                            putOpt(payment, "reference", p.reference());
                            payment.put("paidAt", p.paidAt().toString());
                            putOpt(payment, "cashierName", p.cashierName());
                        });

        return root.toString();
    }

    DocumentSnapshot fromJson(String json) {
        try {
            JsonNode root = M.readTree(json);

            JsonNode i = root.path("institution");
            DocumentSnapshot.Institution institution =
                    new DocumentSnapshot.Institution(
                            i.path("name").asText(),
                            optText(i, "department"),
                            optText(i, "municipality"),
                            optText(i, "legalIdentifier"),
                            optText(i, "address"),
                            optText(i, "phone"),
                            optText(i, "email"),
                            optText(i, "website"),
                            optText(i, "logoPath"));

            JsonNode cu = root.path("customer");
            DocumentSnapshot.Customer customer =
                    new DocumentSnapshot.Customer(
                            cu.path("name").asText(),
                            optText(cu, "identificationType"),
                            optText(cu, "identificationNumber"),
                            optText(cu, "address"),
                            optText(cu, "phone"));

            List<DocumentSnapshot.Line> lines = new ArrayList<>();
            for (JsonNode node : root.path("lines")) {
                lines.add(
                        new DocumentSnapshot.Line(
                                node.path("concept").asText(),
                                optText(node, "description"),
                                node.path("quantity").asLong(),
                                optText(node, "unit"),
                                money(node, "unitPrice"),
                                money(node, "discount"),
                                money(node, "surcharge"),
                                money(node, "lineTotal")));
            }

            JsonNode t = root.path("totals");
            DocumentSnapshot.Totals totals =
                    new DocumentSnapshot.Totals(
                            money(t, "subtotal"),
                            money(t, "discount"),
                            money(t, "surcharge"),
                            money(t, "total"),
                            money(t, "paid"),
                            money(t, "balance"));

            Optional<DocumentSnapshot.PaymentInfo> payment = Optional.empty();
            JsonNode p = root.get("payment");
            if (p != null && !p.isNull()) {
                payment =
                        Optional.of(
                                new DocumentSnapshot.PaymentInfo(
                                        p.path("method").asText(),
                                        money(p, "amount"),
                                        p.has("tendered")
                                                ? Optional.of(money(p, "tendered"))
                                                : Optional.empty(),
                                        p.has("change")
                                                ? Optional.of(money(p, "change"))
                                                : Optional.empty(),
                                        optText(p, "reference"),
                                        Instant.parse(p.path("paidAt").asText()),
                                        optText(p, "cashierName")));
            }

            return new DocumentSnapshot(
                    DocumentKind.valueOf(root.path("kind").asText()),
                    root.path("documentNumber").asText(),
                    Instant.parse(root.path("issuedAt").asText()),
                    institution,
                    customer,
                    lines,
                    totals,
                    payment,
                    optText(root, "reference"),
                    optText(root, "issuedByName"),
                    optText(root, "footerNote"),
                    root.path("verificationCode").asText());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SirmaxException("Could not read the document snapshot", e);
        }
    }

    private static void putMoney(ObjectNode parent, String field, Money money) {
        ObjectNode node = parent.putObject(field);
        node.put("minor", money.minorUnits());
        node.put("currency", money.currency().getCurrencyCode());
    }

    private static Money money(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        return new Money(
                node.path("minor").asLong(),
                Currency.getInstance(node.path("currency").asText("DOP")));
    }

    private static void putOpt(ObjectNode node, String field, Optional<String> value) {
        value.ifPresent(v -> node.put(field, v));
    }

    private static Optional<String> optText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? Optional.empty()
                : Optional.of(value.asText());
    }
}
