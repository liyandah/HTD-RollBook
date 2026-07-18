-- Fix Flyway checksum mismatch for V17 migration
-- Run this script in your PostgreSQL database

-- Update the checksum for V17 migration
UPDATE flyway_schema_history
SET checksum = 2086120007
WHERE version = '17';

-- Verify the update
SELECT version, description, checksum, installed_on
FROM flyway_schema_history
WHERE version = '17';
