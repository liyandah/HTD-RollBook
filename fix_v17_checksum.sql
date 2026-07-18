-- Quick fix for Flyway checksum mismatch on V17
-- Run this in your PostgreSQL database: salvation_army_db

UPDATE flyway_schema_history
SET checksum = 2086120007
WHERE version = '17';

-- Verify it worked
SELECT version, description, checksum, installed_on
FROM flyway_schema_history
WHERE version = '17';
