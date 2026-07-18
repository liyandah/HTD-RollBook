-- Add fields to chat_session for post-verification flow
ALTER TABLE chat_session ADD COLUMN IF NOT EXISTS record_id UUID;
ALTER TABLE chat_session ADD COLUMN IF NOT EXISTS ward VARCHAR(255);
ALTER TABLE chat_session ADD COLUMN IF NOT EXISTS brigade VARCHAR(255);
ALTER TABLE chat_session ADD COLUMN IF NOT EXISTS person_image_uploaded BOOLEAN DEFAULT FALSE;
ALTER TABLE chat_session ADD COLUMN IF NOT EXISTS cert_image_uploaded BOOLEAN DEFAULT FALSE;

-- Create index for record_id lookups
CREATE INDEX IF NOT EXISTS idx_chat_session_record_id ON chat_session(record_id);
