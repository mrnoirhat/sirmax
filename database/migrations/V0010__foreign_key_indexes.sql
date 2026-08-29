-- SPDX-License-Identifier: AGPL-3.0-or-later
-- Indexes for the foreign keys the application actually traverses (Phase 13,
-- migration audit).
--
-- SQLite indexes primary keys but never foreign keys. An unindexed FK costs in
-- two places, both invisible until a municipality has years of data:
--
--   · a lookup by that column becomes a full table scan — "las facturas de este
--     trámite", "las devoluciones de este pago";
--   · deleting a parent row scans the whole child table to enforce the
--     constraint, so ON DELETE SET NULL / RESTRICT gets slower as the archive
--     grows.
--
-- Not every foreign key gets one. Authorship columns (`*_by`, `actor_user_id`)
-- exist so the audit trail can name who did something; nothing queries by them,
-- and an index on each would cost write throughput on the busiest tables in the
-- system to serve a report nobody runs. MigrationAuditTest carries that
-- exemption list, so skipping one is a decision on record rather than an
-- oversight.

-- ── traversed by the application ──
CREATE INDEX ix_refund_payment ON refund(payment_id);
CREATE INDEX ix_registered_document_procedure ON registered_document(procedure_id);
CREATE INDEX ix_issued_document_payment ON issued_document(payment_id);
CREATE INDEX ix_issued_document_registered ON issued_document(registered_document_id);
CREATE INDEX ix_agreement_procedure ON agreement(procedure_id);
CREATE INDEX ix_agreement_transferred_from ON agreement(transferred_from_id);
CREATE INDEX ix_inspection_asset ON inspection(asset_id);
CREATE INDEX ix_user_role_role ON user_role(role_id);
CREATE INDEX ix_role_permission_permission ON role_permission(permission_key);
CREATE INDEX ix_service_definition_department ON service_definition(department_id);

-- ── scanned when a parent row is deleted or restricted ──
-- A service version is RESTRICTed by its procedures; without this, checking
-- whether a version is still in use scans every case in the archive.
CREATE INDEX ix_procedure_service_version ON procedure(service_version_id);
CREATE INDEX ix_invoice_service_definition ON invoice(service_definition_id);
