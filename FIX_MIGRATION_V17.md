# Fix V17 Migration - Missing Columns

## Problem
The migration V17 was marked as applied in Flyway, but the actual columns weren't created in the database. This causes schema validation to fail.

## Solution

Run the SQL script `apply_v17_columns.sql` in your PostgreSQL database:

### Using psql:
```bash
psql -U postgres -d salvation_army_db -f apply_v17_columns.sql
```

### Or using pgAdmin:
1. Connect to `salvation_army_db`
2. Open Query Tool
3. Copy and paste the contents of `apply_v17_columns.sql`
4. Execute

### Or run directly:
```bash
psql -U postgres -d salvation_army_db -c "ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS photo_status VARCHAR(50) DEFAULT 'MISSING', ADD COLUMN IF NOT EXISTS photo_requested_at TIMESTAMP, ADD COLUMN IF NOT EXISTS photo_reviewed_at TIMESTAMP, ADD COLUMN IF NOT EXISTS photo_review_notes TEXT, ADD COLUMN IF NOT EXISTS photo_requested_by UUID REFERENCES users(id);"
```

Then run the UPDATE and CREATE INDEX commands from the script.

## After Running

1. Restart your Spring Boot application
2. The application should start successfully
3. Schema validation will pass

## Verify

After restarting, check the logs - you should see:
```
HHH000204: Processing PersistenceUnitInfo [name: default]
HHH000412: Hibernate ORM core version 6.4.1.Final
```

No more schema validation errors!
