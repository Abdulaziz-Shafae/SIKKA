-- Apply once after checking whether Hibernate already created these columns.
-- Back up production data first.
ALTER TABLE visit
    ADD COLUMN free_slot_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN date_change_used BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reused_from_visit_id INT NULL;

-- reused_from_visit_id is intentionally a numeric snapshot rather than a foreign key.
-- Deleting the original visit must not make a repeated visit undeletable.
-- Before production release, reconcile historical started free visits with the new quota.
-- Do not automatically backfill all started visits: some may already have been paid.
