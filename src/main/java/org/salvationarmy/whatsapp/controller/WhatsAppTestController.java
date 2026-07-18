package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify WhatsApp API token and send test messages
 */
@RestController
@RequestMapping("/api/test")
public class WhatsAppTestController {

    @Autowired
    private WhatsAppService whatsAppService;

    @Value("${whatsapp.meta.access-token}")
    private String accessToken;

    @Value("${whatsapp.meta.phone-number-id}")
    private String phoneNumberId;

    /**
     * Test endpoint to verify token configuration
     * GET /api/test/whatsapp-config
     */
    @GetMapping("/whatsapp-config")
    public ResponseEntity<?> testConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("phoneNumberId", phoneNumberId);
        config.put("accessTokenConfigured", accessToken != null && !accessToken.isEmpty());
        config.put("accessTokenLength", accessToken != null ? accessToken.length() : 0);
        config.put("tokenPreview", accessToken != null && accessToken.length() > 10 
                ? accessToken.substring(0, 10) + "..." 
                : "not configured");
        
        return ResponseEntity.ok(config);
    }

    /**
     * Send a test message to a WhatsApp number
     * POST /api/test/send-message
     * Body: { "to": "+1234567890", "message": "Test message" }
     */
    @PostMapping("/send-message")
    public ResponseEntity<?> sendTestMessage(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String message = payload.get("message");

        if (to == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'to' or 'message' field"));
        }

        try {
            whatsAppService.sendMessage(to, message);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test message sent to " + to
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}





