ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_number_unique
    ON users(phone_number)
    WHERE phone_number IS NOT NULL;
