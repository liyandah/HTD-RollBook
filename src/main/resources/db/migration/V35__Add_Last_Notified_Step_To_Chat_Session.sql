ALTER TABLE chat_session
    ADD COLUMN IF NOT EXISTS last_notified_step VARCHAR(100);
