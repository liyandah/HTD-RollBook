package org.salvationarmy.whatsapp.dto;

import lombok.Data;

@Data
public class BotMessageRequest {
    private String sessionId;
    private String message;
}
