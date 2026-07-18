package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionsOverviewResponse {
    private BigDecimal totalToday;
    private BigDecimal totalThisWeek;
    private BigDecimal totalThisMonth;
    private BigDecimal totalTithes;
    private BigDecimal totalProjects;
    private BigDecimal totalEvents;
    private List<Map<String, Object>> latestPayments;
    private Map<String, BigDecimal> collectionsByCategory;
}
