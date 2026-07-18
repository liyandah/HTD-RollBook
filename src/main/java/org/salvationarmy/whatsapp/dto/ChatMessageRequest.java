package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessageRequest {
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    @NotBlank(message = "Message text is required")
    private String text;
    
    private UUID clientMessageId; // Optional: for idempotency
}
