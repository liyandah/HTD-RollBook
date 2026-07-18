# Fix Flyway Checksum Mismatch for V17

## Problem
Flyway detected that migration V17 was already applied to the database with checksum `0`, but the local file now has checksum `2086120007`. This happens when a migration file is modified after being applied.

## Solution

Run this SQL in your PostgreSQL database to update the checksum:

```sql
-- Update the checksum for V17 migration
UPDATE flyway_schema_history
SET checksum = 2086120007
WHERE version = '17';

-- Verify the update
SELECT version, description, checksum, installed_on
FROM flyway_schema_history
WHERE version = '17';
```

## Alternative: Delete and Re-run

If you prefer to delete the migration record and let it re-run:

```sql
-- Delete the V17 migration record
DELETE FROM flyway_schema_history WHERE version = '17';

-- Then restart the application - Flyway will re-apply V17
```

## Using psql Command Line

```bash
psql -U your_username -d salvation_army_db -c "UPDATE flyway_schema_history SET checksum = 2086120007 WHERE version = '17';"
```

After running the SQL, restart your Spring Boot application.
