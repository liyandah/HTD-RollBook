-- Chat Session Table
CREATE TABLE IF NOT EXISTS chat_session (
    session_id VARCHAR(255) PRIMARY KEY,
    state VARCHAR(50) NOT NULL DEFAULT 'START',
    corps_id INTEGER,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dob DATE,
    age INTEGER,
    id_number VARCHAR(100),
    favorite_song TEXT,
    bible_verse TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat Message Table
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    sender VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_chat_session_state ON chat_session(state);
CREATE INDEX IF NOT EXISTS idx_chat_session_status ON chat_session(status);
CREATE INDEX IF NOT EXISTS idx_chat_session_created ON chat_session(created_at);
CREATE INDEX IF NOT EXISTS idx_chat_message_session ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created ON chat_message(created_at);

-- Update soldier_registration to add chat_session_id if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'soldier_registration' 
        AND column_name = 'chat_session_id'
    ) THEN
        ALTER TABLE soldier_registration ADD COLUMN chat_session_id VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'soldier_registration' 
        AND column_name = 'age'
    ) THEN
        ALTER TABLE soldier_registration ADD COLUMN age INTEGER;
    END IF;
END $$;
