-- Migration V3: Update for High Field Temple features
-- Adds record_code, template_type, ward, brigade
-- Removes id_number

-- 1. Create sequence for record codes
CREATE SEQUENCE IF NOT EXISTS soldier_record_seq START WITH 1;

-- 2. Add record_code column (will be populated after creation)
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS record_code VARCHAR(20);

-- 3. Backfill existing records with record_code in created_at order
DO $$
DECLARE
    rec RECORD;
    seq_val INTEGER := 1;
BEGIN
    FOR rec IN SELECT id FROM soldier_records ORDER BY created_at ASC
    LOOP
        UPDATE soldier_records 
        SET record_code = 'HFT ' || LPAD(seq_val::TEXT, 3, '0')
        WHERE id = rec.id;
        seq_val := seq_val + 1;
    END LOOP;
    
    -- Set sequence to continue from max value
    PERFORM setval('soldier_record_seq', seq_val);
END $$;

-- 4. Make record_code NOT NULL and UNIQUE after backfill
ALTER TABLE soldier_records ALTER COLUMN record_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_soldier_record_code ON soldier_records(record_code);

-- 5. Add template_type column with default 'STANDARD'
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS template_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD';
CREATE INDEX IF NOT EXISTS idx_soldier_template_type ON soldier_records(template_type);

-- 6. Add ward and brigade columns
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS ward VARCHAR(255);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS brigade VARCHAR(255);

-- 7. Add indexes for ward and brigade (optional but useful for filtering)
CREATE INDEX IF NOT EXISTS idx_soldier_ward ON soldier_records(ward);
CREATE INDEX IF NOT EXISTS idx_soldier_brigade ON soldier_records(brigade);

-- 8. Drop id_number column (removed as it's now required)

