package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.PhotoRejectRequest;
import org.salvationarmy.whatsapp.dto.PhotoRequestRequest;
import org.salvationarmy.whatsapp.dto.PhotoStatusResponse;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.PhotoManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/records")
public class AdminPhotoController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminPhotoController.class);
    
    @Autowired
    private PhotoManagementService photoManagementService;
    
    @Autowired
    private SoldierRecordRepository soldierRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Request photo upload/resubmission
     * POST /api/admin/records/{recordId}/photo/request
     */
    @PostMapping("/{recordId}/photo/request")
    public ResponseEntity<?> requestPhoto(
            @PathVariable UUID recordId,
            @Valid @RequestBody PhotoRequestRequest request,
            Authentication authentication) {
        try {
            // Check if user is admin
            UUID adminUserId = getCurrentUserId(authentication);
            User admin = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"ADMIN".equals(admin.getRole())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only administrators can request photo uploads");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            // Verify record exists
            if (!soldierRecordRepository.existsById(recordId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Not Found");
                error.put("message", "Record not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            photoManagementService.requestPhoto(recordId, adminUserId, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Photo request sent successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error requesting photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Unexpected error requesting photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Approve photo
     * POST /api/admin/records/{recordId}/photo/approve
     */
    @PostMapping("/{recordId}/photo/approve")
    public ResponseEntity<?> approvePhoto(
            @PathVariable UUID recordId,
            Authentication authentication) {
        try {
            // Check if user is admin
            UUID adminUserId = getCurrentUserId(authentication);
            User admin = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"ADMIN".equals(admin.getRole())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only administrators can approve photos");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            photoManagementService.approvePhoto(recordId, adminUserId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Photo approved successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error approving photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Unexpected error approving photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Reject photo
     * POST /api/admin/records/{recordId}/photo/reject
     */
    @PostMapping("/{recordId}/photo/reject")
    public ResponseEntity<?> rejectPhoto(
            @PathVariable UUID recordId,
            @Valid @RequestBody PhotoRejectRequest request,
            Authentication authentication) {
        try {
            // Check if user is admin
            UUID adminUserId = getCurrentUserId(authentication);
            User admin = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"ADMIN".equals(admin.getRole())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only administrators can reject photos");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            photoManagementService.rejectPhoto(recordId, adminUserId, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Photo rejected successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error rejecting photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Unexpected error rejecting photo: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        // Try email first
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        // Try username if email lookup failed
        userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        throw new RuntimeException("User not found for identifier: " + identifier);
    }
}
