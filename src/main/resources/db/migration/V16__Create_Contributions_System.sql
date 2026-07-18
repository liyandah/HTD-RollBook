-- Contribution Categories (Tithe, Offering, Building, Camp, Congress, etc.)
CREATE TABLE IF NOT EXISTS contribution_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- TITHE, PROJECT, EVENT
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contribution_category_type ON contribution_categories(type);
CREATE INDEX idx_contribution_category_active ON contribution_categories(active);

-- Projects (Building projects, renovations, etc.)
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_amount DECIMAL(15, 2),
    collected_amount DECIMAL(15, 2) DEFAULT 0,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
    created_by_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_project_status ON projects(status);
CREATE INDEX idx_project_dates ON projects(start_date, end_date);

-- Events (Camps, Congress, etc.)
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(100), -- EASTER_CAMP, YOUTH_CAMP, CONGRESS, etc.
    start_date DATE,
    end_date DATE,
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
    created_by_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_event_status ON events(status);
CREATE INDEX idx_event_dates ON events(start_date, end_date);
CREATE INDEX idx_event_type ON events(event_type);

-- Payments (all contributions)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id UUID NOT NULL, -- References users table (member)
    category_id UUID NOT NULL,
    project_id UUID, -- Nullable, only for PROJECT type
    event_id UUID, -- Nullable, only for EVENT type
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(50) NOT NULL, -- CASH, ECOCASH, BANK_TRANSFER, etc.
    reference_number VARCHAR(100),
    notes TEXT,
    recorded_by_user_id UUID NOT NULL, -- Secretary who recorded it
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_payment_category FOREIGN KEY (category_id) REFERENCES contribution_categories(id),
    CONSTRAINT fk_payment_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_payment_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_payment_member ON payments(member_id);
CREATE INDEX idx_payment_category ON payments(category_id);
CREATE INDEX idx_payment_project ON payments(project_id);
CREATE INDEX idx_payment_event ON payments(event_id);
CREATE INDEX idx_payment_recorded_at ON payments(recorded_at);
CREATE INDEX idx_payment_recorded_by ON payments(recorded_by_user_id);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- Member who receives notification
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP', -- IN_APP, EMAIL, SMS, WHATSAPP
    is_read BOOLEAN NOT NULL DEFAULT false,
    related_payment_id UUID, -- Link to payment if notification is about payment
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notification_payment FOREIGN KEY (related_payment_id) REFERENCES payments(id)
);

CREATE INDEX idx_notification_user ON notifications(user_id);
CREATE INDEX idx_notification_read ON notifications(user_id, is_read);
CREATE INDEX idx_notification_created_at ON notifications(created_at);
CREATE INDEX idx_notification_payment ON notifications(related_payment_id);

-- Seed default contribution categories
INSERT INTO contribution_categories (name, type, description, active) VALUES
    ('Tithe', 'TITHE', 'Regular tithe contributions', true),
    ('Offering', 'TITHE', 'General offerings', true),
    ('Building Fund', 'PROJECT', 'Building and renovation projects', true),
    ('Easter Camp', 'EVENT', 'Easter camp contributions', true),
    ('Youth Camp', 'EVENT', 'Youth camp contributions', true),
    ('Congress', 'EVENT', 'Congress contributions', true),
    ('Special Project', 'PROJECT', 'Other special projects', true)
ON CONFLICT (name) DO NOTHING;
