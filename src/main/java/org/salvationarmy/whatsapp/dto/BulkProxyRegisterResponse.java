package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkProxyRegisterResponse {

    @JsonProperty("total_rows_processed")
    private int totalRowsProcessed;

    @JsonProperty("new_household_heads")
    private int newHouseholdHeads;

    @JsonProperty("dependents_added")
    private int dependentsAdded;

    @JsonProperty("duplicates_blocked")
    private int duplicatesBlocked;

    @JsonProperty("batch_id")
    private String batchId;

    private List<SoldierRecordResponse> records;
}
