CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES soldier_records(id) ON DELETE CASCADE,
    scan_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    corps_location VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_attendance_member_id ON attendance(member_id);
CREATE INDEX IF NOT EXISTS idx_attendance_scan_time ON attendance(scan_time);
