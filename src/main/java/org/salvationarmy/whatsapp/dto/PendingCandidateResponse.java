package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCandidateResponse {
    private UUID id;
    private String recordCode;
    private String fullName;
    private String gender;
    private Integer age;
    private Boolean married;
    private Integer childrenCount;
    private String assignedSection;
    private String status;
    private String registeredBy;
    private LocalDateTime createdAt;
}
