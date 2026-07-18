CREATE TABLE soldier_registration (
    id BIGSERIAL PRIMARY KEY,
    corps_id VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    dob DATE,
    id_number VARCHAR(100),
    favorite_song VARCHAR(255),
    bible_verse VARCHAR(255),
    dialogflow_session TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
