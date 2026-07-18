-- Support proxy (dependent) registration from verified members in web chat
ALTER TABLE registration_profiles ADD COLUMN IF NOT EXISTS proxy_parent_record_code VARCHAR(50);
ALTER TABLE registration_profiles ADD COLUMN IF NOT EXISTS proxy_relationship VARCHAR(50);
