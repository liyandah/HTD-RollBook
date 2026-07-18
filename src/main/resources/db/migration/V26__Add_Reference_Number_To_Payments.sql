-- Add reference_number to payments if missing (e.g. when DB was migrated with older/different schema)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100);
