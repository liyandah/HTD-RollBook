package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.MonthlyStatsResponse;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReportService {

    @Autowired
    private SoldierRecordRepository soldierRecordRepository;

    public MonthlyStatsResponse getMonthlyStats(int monthsBack) {
        List<MonthlyStatsResponse.MonthlyData> monthlyDataList = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = monthsBack - 1; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            String monthName = monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            
            // Count records created in this month
            long totalRecords = soldierRecordRepository.countByCreatedAtBetween(monthStart, monthEnd);
            long verifiedRecords = soldierRecordRepository.countByStatusAndCreatedAtBetween(RecordStatus.VERIFIED, monthStart, monthEnd);
            long inProgressRecords = soldierRecordRepository.countByStatusAndCreatedAtBetween(RecordStatus.IN_PROGRESS, monthStart, monthEnd);
            
            monthlyDataList.add(MonthlyStatsResponse.MonthlyData.builder()
                    .month(monthName)
                    .totalRecords(totalRecords)
                    .verifiedRecords(verifiedRecords)
                    .inProgressRecords(inProgressRecords)
                    .build());
        }
        
        return MonthlyStatsResponse.builder()
                .monthlyData(monthlyDataList)
                .build();
    }
}





