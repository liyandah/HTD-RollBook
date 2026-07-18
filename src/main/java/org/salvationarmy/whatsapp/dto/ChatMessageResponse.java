package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String botReply;
    private String registrationStep;
    private Map<String, Object> profileSnapshot; // Optional: for debugging
    private UUID messageId;
    /** Optional short labels for the client to render as tap-to-send chips. */
    private List<String> quickReplies;

    /** Optional label + value buttons (value is sent as the next message). */
    private List<QuickReplyOption> quickReplyOptions;
}
