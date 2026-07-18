package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecordIdLoginRequest {
    @NotBlank(message = "Record ID is required")
    private String recordId;
}
