-- Add phone_number and address columns to soldier_records
ALTER TABLE soldier_records 
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

ALTER TABLE soldier_records 
ADD COLUMN IF NOT EXISTS address TEXT;

CREATE INDEX IF NOT EXISTS idx_soldier_phone ON soldier_records(phone_number);
