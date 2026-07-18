package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalRecords;
    private long verifiedCount;
    private long declinedCount;
    private long inProgressCount;
    private long completedRecords; // Alias for verifiedCount for frontend compatibility
    private long verifiedRecords; // Alias for verifiedCount for frontend compatibility
    private long under16Count;
    private long age16AndAboveCount;
    private long totalChildren;
    private List<SoldierRecordResponse> latestRecords;
}






