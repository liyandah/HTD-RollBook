ALTER TABLE chat_session
    ADD COLUMN IF NOT EXISTS original_registrant_id UUID,
    ADD COLUMN IF NOT EXISTS family_relation_type VARCHAR(50);
