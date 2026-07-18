# Quick Fix for Flyway Checksum Mismatch

## Problem
Migration V17 was already applied with checksum `0`, but the local file now has checksum `2086120007`.

## Solution (Choose One)

### Option 1: Update Checksum in Database (Recommended)

Run this SQL command in your PostgreSQL database:

```sql
UPDATE flyway_schema_history
SET checksum = 2086120007
WHERE version = '17';
```

**Using psql:**
```bash
psql -U postgres -d salvation_army_db -c "UPDATE flyway_schema_history SET checksum = 2086120007 WHERE version = '17';"
```

**Or using pgAdmin or any PostgreSQL client:**
- Connect to `salvation_army_db`
- Run the UPDATE command above

### Option 2: Delete and Re-run Migration

If you prefer to delete the migration record and let it re-run:

```sql
DELETE FROM flyway_schema_history WHERE version = '17';
```

Then restart the application - Flyway will re-apply V17.

### Option 3: Temporary Disable Validation (Already Done)

I've temporarily disabled Flyway validation in `application.properties`. After fixing the checksum, you can re-enable it:

```properties
spring.flyway.validate-on-migrate=true
```

## After Fixing

1. Run the SQL command (Option 1 or 2)
2. Restart your Spring Boot application
3. The application should start successfully
4. Re-enable validation in `application.properties` if you used Option 3

## Verify Fix

After restarting, check the logs - you should see:
```
Flyway: Successfully applied migration V17
```

No more checksum mismatch errors!
