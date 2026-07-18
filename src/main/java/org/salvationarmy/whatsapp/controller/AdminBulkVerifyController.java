package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.AdminVerifyBulkRequest;
import org.salvationarmy.whatsapp.dto.PendingCandidateResponse;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminBulkVerifyController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/pending-candidates")
    public ResponseEntity<?> pendingCandidates(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden("Only administrators can access pending candidates");
        }
        List<PendingCandidateResponse> rows = recordService.getPendingCandidates();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verifyCandidate(@PathVariable UUID id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden("Only administrators can verify candidates");
        }
        try {
            return ResponseEntity.ok(recordService.verifyCandidate(id));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/verify-bulk")
    public ResponseEntity<?> verifyBulk(@Valid @RequestBody AdminVerifyBulkRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden("Only administrators can verify records");
        }
        try {
            Map<String, Object> body = recordService.verifyBulkHousehold(request);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to verify records");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        String identifier = authentication.getName();
        Optional<User> user = userRepository.findByEmailIgnoreCase(identifier);
        if (user.isEmpty()) {
            user = userRepository.findByUsernameIgnoreCase(identifier);
        }
        return user.map(u -> "ADMIN".equalsIgnoreCase(u.getRole())).orElse(false);
    }

    private ResponseEntity<Map<String, String>> forbidden(String msg) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", msg);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
