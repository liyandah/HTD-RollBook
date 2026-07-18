package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID id;
    private String type; // BOT or DIRECT
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private long unreadCount;
    private UserInfo otherParticipant; // For DIRECT conversations
    private LocalDateTime createdAt;
}
