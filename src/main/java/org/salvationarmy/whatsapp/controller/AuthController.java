package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.*;
import org.salvationarmy.whatsapp.service.AuthService;
import org.salvationarmy.whatsapp.service.OtpService;
import org.salvationarmy.whatsapp.util.JwtUtil;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SoldierRecordRepository soldierRecordRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            otpService.sendOtp(request.getEmail());
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP sent successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid request");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            String msg = e.getMessage() != null ? e.getMessage() : "";
            boolean rateLimit = msg.contains("Please wait") || msg.contains("seconds before");
            error.put("error", rateLimit ? "Rate limit exceeded" : "OTP email not available");
            error.put("message", msg);
            HttpStatus status = rateLimit ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to send OTP");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            User user = otpService.verifyOtp(request.getEmail(), request.getOtp());
            String token = jwtUtil.generateTokenForEmail(user.getEmail());
            
            // Check if user needs to create password
            boolean requiresPassword = user.getPassword() == null || user.getPassword().trim().isEmpty();
            
            VerifyOtpResponse response = new VerifyOtpResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    requiresPassword
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid OTP");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Verification failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Verification failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login-with-record-id")
    public ResponseEntity<?> loginWithRecordId(@Valid @RequestBody RecordIdLoginRequest request) {
        try {
            // Find soldier record by record code
            SoldierRecord record = soldierRecordRepository.findByRecordCode(request.getRecordId())
                    .orElseThrow(() -> new RuntimeException("Record ID not found"));

            // Get or create user for this record
            User user;
            if (record.getUserId() != null) {
                user = userRepository.findById(record.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } else {
                // Create new user from record
                user = new User();
                user.setEmail(generateEmailFromRecord(record));
                user.setFullName((record.getFirstName() != null ? record.getFirstName() : "") + 
                                (record.getFamilyName() != null ? " " + record.getFamilyName() : "").trim());
                user.setRole("VIEWER");
                user.setStatus("ACTIVE");
                user = userRepository.save(user);

                // Link record to user
                record.setUserId(user.getId());
                soldierRecordRepository.save(record);
            }

            // Check if user needs to create password
            boolean requiresPassword = user.getPassword() == null || user.getPassword().trim().isEmpty();
            
            // Generate JWT token
            String token = jwtUtil.generateTokenForEmail(user.getEmail());

            VerifyOtpResponse response = new VerifyOtpResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    requiresPassword
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed");
            error.put("message", "An error occurred during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/create-password")
    public ResponseEntity<?> createPassword(
            @Valid @RequestBody CreatePasswordRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate password match
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Passwords do not match");
                error.put("message", "Password and confirm password must match");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Set password
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password created successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create password");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create password");
            error.put("message", "An error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/check-password")
    public ResponseEntity<?> checkPassword(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean requiresPassword = user.getPassword() == null || user.getPassword().trim().isEmpty();
            Map<String, Object> response = new HashMap<>();
            response.put("requiresPassword", requiresPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check password");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private String generateEmailFromRecord(SoldierRecord record) {
        String base = (record.getFirstName() != null ? record.getFirstName().toLowerCase() : "user") +
                      (record.getFamilyName() != null ? "." + record.getFamilyName().toLowerCase() : "");
        base = base.replaceAll("[^a-z0-9.]", "");
        return base + "@record.salvationarmy.org";
    }
}






