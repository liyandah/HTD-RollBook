package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.dto.DashboardStatsResponse;
import org.salvationarmy.whatsapp.dto.MonthlyStatsResponse;
import org.salvationarmy.whatsapp.service.RecordService;
import org.salvationarmy.whatsapp.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private RecordService recordService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(recordService.getDashboardStats());
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStatsResponse> getMonthlyStats(
            @RequestParam(defaultValue = "6") int months) {
        MonthlyStatsResponse stats = reportService.getMonthlyStats(months);
        return ResponseEntity.ok(stats);
    }
}





