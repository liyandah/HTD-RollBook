package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderUserId;
    private String senderName;
    private String messageType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Boolean bot; // true if senderUserId is null
}
