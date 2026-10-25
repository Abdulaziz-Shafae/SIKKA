-- Apply once before deploying changes that introduce repeat visit and per-visit free entitlement.
-- Back up the database before applying. These columns must also exist in ORM model.
ALTER TABLE visit
    ADD COLUMN free_slot_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reused_from_visit_id INT NULL;

ALTER TABLE visit
    ADD CONSTRAINT fk_visit_reused_from FOREIGN KEY (reused_from_visit_id) REFERENCES visit(id);
-- Historical successful free generations require careful account-by-account
-- migration to the new two-visit quota before production rollout.
