package org.salvationarmy.whatsapp.dto;

import lombok.Data;

@Data
public class AttendanceMarkRequest {
    private String memberId;
    private String timestamp;
    private String corpsLocation;
}
