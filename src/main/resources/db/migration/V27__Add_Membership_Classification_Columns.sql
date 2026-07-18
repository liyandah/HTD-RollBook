-- Fellowship / classification (Excel bulk + admin edits)
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS marital_status VARCHAR(40);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS kids_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS department VARCHAR(100);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS brigade_eligibility VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_soldier_department ON soldier_records (department);
CREATE INDEX IF NOT EXISTS idx_soldier_id_number_lookup ON soldier_records (id_number) WHERE id_number IS NOT NULL AND TRIM(id_number) <> '';
