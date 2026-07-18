package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
}
