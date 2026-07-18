package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AdminVerifyBulkRequest {

    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("verify_status")
    private String verifyStatus;

    @JsonProperty("record_ids")
    private List<String> recordIds;

    /** National ID of the household head (same value stored on dependents as proxy_id). */
    @JsonProperty("proxy_id")
    private String proxyId;

    @JsonProperty("admin_notes")
    private String adminNotes;
}
