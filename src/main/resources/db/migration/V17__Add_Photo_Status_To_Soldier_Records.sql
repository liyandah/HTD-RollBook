-- Migration: Add photo status fields to soldier_records table
-- This allows admins to request, approve, or reject user photos

-- Add photo status enum type
DO $$ BEGIN
    CREATE TYPE photo_status_enum AS ENUM ('MISSING', 'UPLOADED', 'APPROVED', 'REJECTED', 'RESUBMIT_REQUESTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

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

-- Create index for photo status queries
CREATE INDEX IF NOT EXISTS idx_soldier_photo_status ON soldier_records(photo_status);

-- Create index for photo requested by queries
CREATE INDEX IF NOT EXISTS idx_soldier_photo_requested_by ON soldier_records(photo_requested_by);
