-- Quick Fix: Manually apply V17 migration columns
-- The migration was marked as applied but columns weren't created
-- Run this SQL script in your PostgreSQL database

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

-- Verify columns were added
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'soldier_records'
  AND column_name IN ('photo_status', 'photo_requested_at', 'photo_reviewed_at', 'photo_review_notes', 'photo_requested_by')
ORDER BY column_name;
