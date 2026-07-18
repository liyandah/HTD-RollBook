-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    wa_id VARCHAR(50) UNIQUE NOT NULL,
    state VARCHAR(50) NOT NULL,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_conversation_wa_id ON conversations(wa_id);

-- Create soldier_records table
CREATE TABLE soldier_records (
    id UUID PRIMARY KEY,
    wa_id VARCHAR(50) NOT NULL,
    corps_name VARCHAR(255),
    enrolled_corps_name VARCHAR(255),
    first_name VARCHAR(100),
    family_name VARCHAR(100),
    dob DATE,
    age INTEGER,
    id_number VARCHAR(50),
    favorite_song TEXT,
    favorite_bible_verse TEXT,
    person_image_path VARCHAR(500),
    cert_image_path VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_soldier_wa_id ON soldier_records(wa_id);
CREATE INDEX idx_soldier_status ON soldier_records(status);
CREATE INDEX idx_soldier_created_at ON soldier_records(created_at);






