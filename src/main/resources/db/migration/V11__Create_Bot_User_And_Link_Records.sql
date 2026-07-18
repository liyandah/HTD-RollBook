-- Create system bot user
INSERT INTO users (id, email, full_name, role, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001'::uuid,
    'bot@salvationarmy.org',
    'HTF Data collection Bot',
    'VIEWER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Add user_id column to soldier_records to link records to users
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_soldier_user_id ON soldier_records(user_id);

-- Add foreign key constraint
ALTER TABLE soldier_records 
ADD CONSTRAINT fk_soldier_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
