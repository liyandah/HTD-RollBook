-- Revert status column to TEXT so Hibernate EnumType.STRING works cleanly
-- and avoid PostgreSQL enum vs varchar operator mismatch.

-- Change column type back to TEXT, casting from the enum type
ALTER TABLE soldier_records
    ALTER COLUMN status TYPE TEXT USING status::TEXT;

-- Ensure default value remains IN_PROGRESS as plain text
ALTER TABLE soldier_records
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS';

-- Drop the custom enum type if no other column depends on it
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'record_status') THEN
        DROP TYPE record_status;
    END IF;
END
$$;

