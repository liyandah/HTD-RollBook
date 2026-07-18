package org.salvationarmy.whatsapp.dto;

import org.salvationarmy.whatsapp.entity.RecordStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private RecordStatus status;

    private String declineReason;
}






