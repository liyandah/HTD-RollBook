-- Fix V17 Migration Issue
-- The migration was marked as applied but columns weren't created
-- Run this to delete the migration record and let it re-run

-- Step 1: Delete the V17 migration record
DELETE FROM flyway_schema_history WHERE version = '17';

-- Step 2: Manually add the columns (in case migration doesn't re-run)
-- This ensures the columns exist even if Flyway doesn't re-apply

-- Add photo status fields to soldier_records
ALTER TABLE soldier_records
    ADD COLUMN IF NOT EXISTS photo_status VARCHAR(50) DEFAULT 'MISSING',
    ADD COLUMN IF NOT EXISTS photo_requested_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS photo_reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS photo_review_notes TEXT,
    ADD COLUMN IF NOT EXISTS photo_requested_by UUID REFERENCES users(id);

-- Set default photo_status based on person_image_path
UPDATE soldier_records
SET photo_status = CASE
    WHEN person_image_path IS NULL OR person_image_path = '' THEN 'MISSING'
    ELSE 'UPLOADED'
END
WHERE photo_status = 'MISSING' OR photo_status IS NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_soldier_photo_status ON soldier_records(photo_status);
CREATE INDEX IF NOT EXISTS idx_soldier_photo_requested_by ON soldier_records(photo_requested_by);

-- Step 3: Re-insert the migration record with correct checksum
-- This prevents Flyway from trying to re-run it
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    COALESCE(MAX(installed_rank), 0) + 1,
    '17',
    'Add Photo Status To Soldier Records',
    'SQL',
    'V17__Add_Photo_Status_To_Soldier_Records.sql',
    2086120007,
    'postgres',
    CURRENT_TIMESTAMP,
    0,
    true
FROM flyway_schema_history;
