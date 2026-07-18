ALTER TABLE chat_session
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS next_of_kin_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS next_of_kin_phone VARCHAR(20);
