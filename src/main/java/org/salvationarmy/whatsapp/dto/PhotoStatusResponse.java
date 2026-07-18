package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.salvationarmy.whatsapp.entity.SoldierRecord;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoStatusResponse {
    private SoldierRecord.PhotoStatus status;
    private String photoUrl;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private UUID requestedByUserId;
    private String requestedByUserName;
    private boolean requiresAction; // true if user needs to upload/resubmit
}
