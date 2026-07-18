ALTER TABLE soldier_records
    ADD COLUMN IF NOT EXISTS primary_registrant_id UUID,
    ADD COLUMN IF NOT EXISTS registration_relation VARCHAR(50);
