package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Label shown in chat; value sent as the next user message (hidden token or plain text). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickReplyOption {
    private String label;
    private String value;
}
