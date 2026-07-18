package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;
    
    private UUID clientMessageId; // Optional: for idempotency
}
