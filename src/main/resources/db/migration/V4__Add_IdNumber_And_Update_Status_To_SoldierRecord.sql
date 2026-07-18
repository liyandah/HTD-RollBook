DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'record_status') THEN
        CREATE TYPE record_status AS ENUM ('IN_PROGRESS', 'VERIFIED');
    END IF;
END
$$;

ALTER TABLE soldier_records
    ADD COLUMN IF NOT EXISTS id_number VARCHAR(50);

-- Temporarily alter column type to text to allow for updating values
ALTER TABLE soldier_records
    ALTER COLUMN status TYPE TEXT;

-- Update existing 'COMPLETE' status values to 'VERIFIED'
UPDATE soldier_records
SET status = 'VERIFIED'
WHERE status = 'COMPLETE';

-- Drop any existing default before changing the type
ALTER TABLE soldier_records
    ALTER COLUMN status DROP DEFAULT;

-- Now alter the column to the new ENUM type
ALTER TABLE soldier_records
    ALTER COLUMN status TYPE record_status USING status::record_status;

-- Re-apply default for status
ALTER TABLE soldier_records
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS'::record_status;