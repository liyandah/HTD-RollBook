-- Make username and password nullable for OTP-based users
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Create OTP codes table
CREATE TABLE IF NOT EXISTS otp_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_email ON otp_codes(email);
CREATE INDEX idx_otp_expires_at ON otp_codes(expires_at);

-- Create conversations table (new structure for WhatsApp-style chat)
CREATE TABLE IF NOT EXISTS conversations_new (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL, -- BOT or DIRECT
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversation_type ON conversations_new(type);
CREATE INDEX idx_conversation_last_message ON conversations_new(last_message_at);

-- Create conversation participants table
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_conv_participant_conv FOREIGN KEY (conversation_id) REFERENCES conversations_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_participant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_conv_participant_user ON conversation_participants(user_id);

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_user_id UUID, -- NULL for bot messages
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT or SYSTEM
    content TEXT NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conv FOREIGN KEY (conversation_id) REFERENCES conversations_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_message_conv ON messages(conversation_id);
CREATE INDEX idx_message_sender ON messages(sender_user_id);
CREATE INDEX idx_message_created ON messages(created_at);

-- Create registration profiles table
CREATE TABLE IF NOT EXISTS registration_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    national_id VARCHAR(50),
    address TEXT,
    next_of_kin_name VARCHAR(100),
    next_of_kin_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT or COMPLETED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reg_profile_user ON registration_profiles(user_id);
CREATE INDEX idx_reg_profile_status ON registration_profiles(status);
