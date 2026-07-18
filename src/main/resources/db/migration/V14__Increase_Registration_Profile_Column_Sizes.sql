-- Increase column sizes for registration_profiles to accommodate temporary data storage
-- The bot stores multiple pieces of data with pipe separators during registration flow

ALTER TABLE registration_profiles 
ALTER COLUMN next_of_kin_name TYPE TEXT;

ALTER TABLE registration_profiles 
ALTER COLUMN next_of_kin_phone TYPE TEXT;

-- Also increase national_id if needed (for storing age temporarily)
ALTER TABLE registration_profiles 
ALTER COLUMN national_id TYPE VARCHAR(100);
