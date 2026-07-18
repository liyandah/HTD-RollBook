ALTER TABLE soldier_records
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

UPDATE soldier_records
SET is_active = TRUE
WHERE is_active IS NULL;
