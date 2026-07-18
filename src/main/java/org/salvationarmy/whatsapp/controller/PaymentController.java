package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.CapturePaymentRequest;
import org.salvationarmy.whatsapp.dto.PaymentResponse;
import org.salvationarmy.whatsapp.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private org.salvationarmy.whatsapp.repository.UserRepository userRepository;

    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        // Try email first
        java.util.Optional<org.salvationarmy.whatsapp.entity.User> userOpt = userRepository.findByEmail(identifier);
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

    @PostMapping
    public ResponseEntity<?> capturePayment(
            @Valid @RequestBody CapturePaymentRequest request,
            Authentication authentication) {
        try {
            UUID recordedByUserId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN, SECRETARY, or TREASURER)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(recordedByUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String role = user.getRole();
            if (!role.equals("ADMIN") && !role.equals("SECRETARY") && !role.equals("TREASURER")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN, SECRETARY, or TREASURER can capture payments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            PaymentResponse response = paymentService.capturePayment(request, recordedByUserId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to capture payment");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPayments(pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPayment(@PathVariable UUID id) {
        try {
            PaymentResponse payment = paymentService.getPaymentById(id);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Payment not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByMember(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPaymentsByMember(memberId, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPaymentsByCategory(categoryId, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPaymentsByProject(projectId, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByEvent(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPaymentsByEvent(eventId, pageable);
        return ResponseEntity.ok(payments);
    }
}
