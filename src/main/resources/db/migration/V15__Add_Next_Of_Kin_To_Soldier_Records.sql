-- Add next_of_kin_name and next_of_kin_phone columns to soldier_records
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS next_of_kin_name VARCHAR(100);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS next_of_kin_phone VARCHAR(20);
