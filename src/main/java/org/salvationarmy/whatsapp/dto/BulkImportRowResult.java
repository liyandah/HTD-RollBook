package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportRowResult {
    /** 1-based row index in the uploaded sheet batch */
    private int index;
    /** BLOCKED, WARNING, or CLEAN */
    private String category;
    /** e.g. DUPLICATE_NATIONAL_ID, DUPLICATE_ID_IN_FILE, NAME_MATCH, CLEAN */
    private String code;
    private String message;
    private CreateRecordRequest row;
}
