-- Link dependents to proxy registrant (national ID of household head)
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS proxy_id VARCHAR(50);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS relationship VARCHAR(50);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS household_batch_id VARCHAR(64);
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS household_admin_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_soldier_proxy_id ON soldier_records (proxy_id) WHERE proxy_id IS NOT NULL AND TRIM(proxy_id) <> '';
CREATE INDEX IF NOT EXISTS idx_soldier_household_batch ON soldier_records (household_batch_id) WHERE household_batch_id IS NOT NULL AND TRIM(household_batch_id) <> '';

-- Reporting view: dependents joined to proxy row by national ID
CREATE OR REPLACE VIEW view_household_records AS
SELECT
    r.id,
    r.record_code AS record_id,
    TRIM(CONCAT(COALESCE(r.first_name, ''), ' ', COALESCE(r.family_name, ''))) AS full_name,
    r.age,
    r.gender,
    r.department,
    r.status AS status,
    r.proxy_id,
    r.relationship,
    p.first_name AS proxy_first_name,
    p.family_name AS proxy_family_name,
    TRIM(CONCAT(COALESCE(p.first_name, ''), ' ', COALESCE(p.family_name, ''))) AS registered_by_name,
    p.phone_number AS proxy_contact
FROM soldier_records r
LEFT JOIN soldier_records p
    ON r.proxy_id IS NOT NULL
   AND TRIM(LOWER(r.proxy_id)) = TRIM(LOWER(p.id_number));
