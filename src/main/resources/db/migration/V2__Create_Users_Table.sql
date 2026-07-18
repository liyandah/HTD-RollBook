-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, full_name, role, status)
VALUES (
    'admin',
    'admin@salvationarmy.org',
    '$2a$10$XPTyZ6hCL3KGEemqZyh8LuGYvN7xLZJGN3p.1j7xkG8qJd5lYvGYa', -- BCrypt hash of 'admin123'
    'Administrator',
    'ADMIN',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;





