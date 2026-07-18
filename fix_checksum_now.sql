-- IMMEDIATE FIX: Update Flyway checksum for V17
-- Run this in your PostgreSQL database RIGHT NOW

UPDATE flyway_schema_history
SET checksum = 2086120007
WHERE version = '17';

-- Verify it worked
SELECT version, description, checksum, installed_on
FROM flyway_schema_history
WHERE version = '17';
