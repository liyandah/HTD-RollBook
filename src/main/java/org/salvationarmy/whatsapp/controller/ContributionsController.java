package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.dto.ContributionsOverviewResponse;
import org.salvationarmy.whatsapp.service.ContributionsOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contributions")
public class ContributionsController {

    @Autowired
    private ContributionsOverviewService overviewService;

    @GetMapping("/overview")
    public ResponseEntity<ContributionsOverviewResponse> getOverview() {
        return ResponseEntity.ok(overviewService.getOverview());
    }
}
