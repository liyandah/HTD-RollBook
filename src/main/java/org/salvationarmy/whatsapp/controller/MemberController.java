package org.salvationarmy.whatsapp.controller;

import lombok.RequiredArgsConstructor;
import org.salvationarmy.whatsapp.dto.ReuploadRequest;
import org.salvationarmy.whatsapp.service.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final RecordService recordService;

    @PatchMapping("/{id}/request-upload")
    public ResponseEntity<Map<String, Object>> requestUpload(@PathVariable UUID id) {
        boolean notified = recordService.requestCertificateUpload(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "notified", notified,
                "message", notified
                        ? "Re-upload requested and member notified."
                        : "Re-upload requested. Chat session not found for notification."
        ));
    }

    @PatchMapping("/{id}/reupload")
    public ResponseEntity<Map<String, Object>> requestReupload(
            @PathVariable UUID id,
            @RequestBody(required = false) ReuploadRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        boolean notified = recordService.requestCertificateReupload(id, reason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "notified", notified,
                "status", "REUPLOAD_REQUIRED",
                "message", notified
                        ? "Photo re-upload requested and member notified."
                        : "Photo re-upload requested. Chat session not found for notification."
        ));
    }
}
