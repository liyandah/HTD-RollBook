package org.salvationarmy.whatsapp.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.salvationarmy.whatsapp.dto.BulkProxyRegisterRequest;
import org.salvationarmy.whatsapp.dto.BulkProxyRegisterResponse;
import org.salvationarmy.whatsapp.dto.BulkValidateResponse;
import org.salvationarmy.whatsapp.dto.CreateRecordRequest;
import org.salvationarmy.whatsapp.dto.DashboardStatsResponse;
import org.salvationarmy.whatsapp.dto.SoldierRecordResponse;
import org.salvationarmy.whatsapp.dto.StatusUpdateRequest;
import org.salvationarmy.whatsapp.dto.UpdateRecordRequest;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.salvationarmy.whatsapp.service.RecordService;
import org.salvationarmy.whatsapp.util.CorpsNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @GetMapping({"/dashboard", "/dashboard-stats"})
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(recordService.getDashboardStats());
    }

    @GetMapping
    public ResponseEntity<Page<SoldierRecordResponse>> getRecords(
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<SoldierRecordResponse> records = recordService.getRecords(
                status != null ? status.name() : null, from, to, q, department, page, size);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoldierRecordResponse> getRecordById(@PathVariable UUID id) {
        SoldierRecordResponse record = recordService.getRecordById(id);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{id}/connections")
    public ResponseEntity<Map<String, List<SoldierRecordResponse>>> getConnections(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.getConnections(id));
    }

    @PostMapping
    public ResponseEntity<?> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        try {
            SoldierRecordResponse response = recordService.createRecord(
                request.getWaId(),
                request.getCorpsName(),
                request.getEnrolledCorpsName(),
                request.getWard(),
                request.getBrigade(),
                request.getFirstName(),
                request.getFamilyName(),
                request.getDob(),
                request.getIdNumber(),
                request.getGender(),
                request.getMaritalStatus(),
                request.getKidsCount(),
                request.getFavoriteSong(),
                request.getFavoriteBibleVerse(),
                request.getStatus() != null ? request.getStatus().name() : null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create record");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/bulk-validate")
    public ResponseEntity<BulkValidateResponse> bulkValidate(@RequestBody List<CreateRecordRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return ResponseEntity.ok(BulkValidateResponse.builder().build());
        }
        return ResponseEntity.ok(recordService.validateBulkImportRows(rows));
    }

    @PostMapping("/recalculate-departments")
    public ResponseEntity<Map<String, Object>> recalculateDepartments() {
        int updated = recordService.recalculateAllMembershipClassifications();
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Membership classification recalculated for all records.");
        body.put("recordsUpdated", updated);
        return ResponseEntity.ok(body);
    }

    /**
     * Chatbot / external: one proxy registers multiple dependents (IN_PROGRESS until admin verifies household).
     */
    @PostMapping("/register-bulk")
    public ResponseEntity<?> registerBulkProxy(@Valid @RequestBody BulkProxyRegisterRequest request) {
        try {
            BulkProxyRegisterResponse response = recordService.registerBulkProxyHousehold(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to register household");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> bulkAddRecords(@RequestBody List<CreateRecordRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", "No records provided for bulk import");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        int successCount = 0;
        List<Map<String, Object>> failures = new java.util.ArrayList<>();
        Map<String, Integer> departmentCounts = new LinkedHashMap<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateRecordRequest request = requests.get(i);
            try {
                SoldierRecordResponse created = recordService.createAdminBulkVerifiedRecord(
                        request.getWaId(),
                        request.getCorpsName(),
                        request.getEnrolledCorpsName(),
                        request.getWard(),
                        request.getBrigade(),
                        request.getFirstName(),
                        request.getFamilyName(),
                        request.getDob(),
                        request.getIdNumber(),
                        request.getGender(),
                        request.getMaritalStatus(),
                        request.getKidsCount(),
                        request.getFavoriteSong(),
                        request.getFavoriteBibleVerse()
                );
                successCount++;
                String dept = created.getDepartment() != null ? created.getDepartment() : "Unknown";
                departmentCounts.merge(dept, 1, Integer::sum);
            } catch (Exception e) {
                Map<String, Object> failure = new HashMap<>();
                failure.put("row", i + 1);
                failure.put("message", e.getMessage());
                failures.add(failure);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", requests.size());
        result.put("created", successCount);
        result.put("failed", failures.size());
        result.put("failures", failures);
        result.put("departmentBreakdown", departmentCounts);

        HttpStatus status = failures.isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request) {
        try {
            String resolvedPhone = request.getPhoneNumber();
            String resolvedAddress = request.getAddress();
            String personImagePath = normalizeImagePath(request.getPersonImageUrl());
            String certImagePath = normalizeImagePath(request.getCertImageUrl());

            SoldierRecordResponse response = recordService.updateRecord(
                id,
                request.getCorpsName(),
                request.getEnrolledCorpsName(),
                request.getWard(),
                request.getBrigade(),
                request.getFirstName(),
                request.getFamilyName(),
                request.getDob(),
                request.getIdNumber(),
                resolvedPhone,
                resolvedAddress,
                request.getGender(),
                request.getMaritalStatus(),
                request.getKidsCount(),
                request.getNextOfKinName(),
                request.getNextOfKinPhone(),
                request.getFavoriteSong(),
                request.getFavoriteBibleVerse(),
                personImagePath,
                certImagePath,
                request.getStatus() != null ? request.getStatus().name() : null
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update record");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    private String normalizeImagePath(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String value = input.trim();
        if (value.startsWith("/api/images/")) {
            return value.substring("/api/images/".length());
        }
        if (value.startsWith("/uploads/")) {
            return value.substring("/uploads/".length());
        }
        return value;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SoldierRecordResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        SoldierRecordResponse updated = recordService.updateStatus(id, request.getStatus(), request.getDeclineReason());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/request-certificate-upload")
    public ResponseEntity<Map<String, Object>> requestCertificateUpload(@PathVariable UUID id) {
        boolean pushed = recordService.requestCertificateUpload(id);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("chatNotified", pushed);
        body.put("message", pushed
                ? "Certificate re-upload request sent to chatbot session."
                : "Certificate marked missing. No active chatbot session was found.");
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable UUID id) {
        try {
            recordService.deleteRecord(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Record deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete record");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/export.csv")
    public void exportRecords(
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"soldier_records.csv\"");

        List<SoldierRecordResponse> records = recordService.getAllRecordsForExport(
                status != null ? status.name() : null, from, to, q, department);

        PrintWriter writer = response.getWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Record ID", "First Name", "Family Name", "Department", "Brigade Eligibility",
                        "Corps Name", "Ward", "Brigade", "ID Number", "Gender", "Marital Status", "Kids Count",
                        "Created At", "Date of Birth", "Status", "WhatsApp ID", "Enrolled Corps",
                        "Age", "Favorite Song", "Favorite Bible Verse"))) {

            for (SoldierRecordResponse record : records) {
                // Map Corps display name (presentation only - legacy names -> "Highfield Temple")
                String corpsDisplayName = CorpsNameUtil.normalize(record.getCorpsName());
                
                csvPrinter.printRecord(
                        record.getRecordCode(),
                        record.getFirstName(),
                        record.getFamilyName(),
                        record.getDepartment() != null ? record.getDepartment() : "N/A",
                        record.getBrigadeEligibility() != null ? record.getBrigadeEligibility() : "N/A",
                        corpsDisplayName,
                        record.getWard() != null ? record.getWard() : "N/A",
                        record.getBrigade() != null ? record.getBrigade() : "N/A",
                        record.getIdNumber() != null ? record.getIdNumber() : "N/A",
                        record.getGender() != null ? record.getGender() : "N/A",
                        record.getMaritalStatus() != null ? record.getMaritalStatus() : "N/A",
                        record.getKidsCount() != null ? record.getKidsCount() : 0,
                        record.getCreatedAt(),
                        record.getDob(),
                        record.getStatus(),
                        record.getWaId(),
                        record.getEnrolledCorpsName(),
                        record.getAge(),
                        record.getFavoriteSong(),
                        record.getFavoriteBibleVerse()
                );
            }

            csvPrinter.flush();
        }
    }
}


