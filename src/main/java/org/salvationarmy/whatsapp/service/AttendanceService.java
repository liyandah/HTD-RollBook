package org.salvationarmy.whatsapp.service;

import lombok.RequiredArgsConstructor;
import org.salvationarmy.whatsapp.dto.AttendanceMarkRequest;
import org.salvationarmy.whatsapp.entity.AttendanceRecord;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.repository.AttendanceRecordRepository;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SoldierRecordRepository soldierRecordRepository;

    public Map<String, Object> markAttendance(AttendanceMarkRequest request) {
        if (request == null || request.getMemberId() == null || request.getMemberId().trim().isEmpty()) {
            throw new IllegalArgumentException("memberId is required");
        }
        UUID memberId = UUID.fromString(request.getMemberId().trim());
        SoldierRecord member = soldierRecordRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        LocalDateTime scanTime = parseTimestamp(request.getTimestamp());
        AttendanceRecord attendance = new AttendanceRecord();
        attendance.setMemberId(memberId);
        attendance.setScanTime(scanTime);
        attendance.setCorpsLocation(
                request.getCorpsLocation() != null && !request.getCorpsLocation().trim().isEmpty()
                        ? request.getCorpsLocation().trim()
                        : member.getCorpsName()
        );
        attendanceRecordRepository.save(attendance);

        String name = ((member.getFirstName() != null ? member.getFirstName() : "") + " " +
                (member.getFamilyName() != null ? member.getFamilyName() : "")).trim();
        return Map.of(
                "success", true,
                "memberId", memberId.toString(),
                "memberName", name.isEmpty() ? "Member" : name,
                "scanTime", scanTime.toString(),
                "message", (name.isEmpty() ? "Member" : name) + " marked as Present!"
        );
    }

    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null || ts.trim().isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(ts.trim().replace("Z", ""));
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.now();
        }
    }
}
