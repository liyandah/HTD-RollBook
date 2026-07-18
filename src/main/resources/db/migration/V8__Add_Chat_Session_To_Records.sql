-- Add chat_session_id to soldier_records to link records with chat sessions
ALTER TABLE soldier_records ADD COLUMN IF NOT EXISTS chat_session_id VARCHAR(255);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_soldier_record_chat_session ON soldier_records(chat_session_id);
