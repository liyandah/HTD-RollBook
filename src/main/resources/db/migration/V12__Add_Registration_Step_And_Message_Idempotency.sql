-- Add registration_step column to registration_profiles
ALTER TABLE registration_profiles 
ADD COLUMN IF NOT EXISTS registration_step VARCHAR(50) DEFAULT 'ASK_PHONE';

CREATE INDEX IF NOT EXISTS idx_reg_profile_step ON registration_profiles(registration_step);

-- Create table for message idempotency (prevent duplicate processing)
CREATE TABLE IF NOT EXISTS processed_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    client_message_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_content TEXT,
    CONSTRAINT fk_processed_msg_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_processed_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations_new(id) ON DELETE CASCADE
);

CREATE INDEX idx_processed_msg_user_client ON processed_messages(user_id, client_message_id);
CREATE INDEX idx_processed_msg_created ON processed_messages(processed_at);

-- Clean up old processed messages (older than 30 minutes) - will be done by scheduled job
-- For now, just create the table
