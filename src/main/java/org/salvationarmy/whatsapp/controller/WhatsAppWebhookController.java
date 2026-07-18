package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.dto.WhatsAppWebhookRequest;
import org.salvationarmy.whatsapp.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService whatsAppService;

    @Value("${whatsapp.verifyToken}")
    private String verifyToken;

    public WhatsAppWebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    // ✅ Health endpoint for testing
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // ✅ GET endpoint for Meta verification
    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        logger.info("Webhook verification request: mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && verifyToken.equals(token) && challenge != null) {
            logger.info("✅ Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        logger.warn("❌ Webhook verification failed");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid token");
    }

    // ✅ POST endpoint to receive messages
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest request) {
        try {
            if (request.getEntry() == null || request.getEntry().isEmpty()) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            for (WhatsAppWebhookRequest.Entry entry : request.getEntry()) {
                if (entry.getChanges() == null)
                    continue;

                for (WhatsAppWebhookRequest.Change change : entry.getChanges()) {
                    if (change.getValue() == null || change.getValue().getMessages() == null)
                        continue;

                    for (WhatsAppWebhookRequest.Message message : change.getValue().getMessages()) {
                        String waId = message.getFrom();
                        String messageType = message.getType();

                        String textBody = null;
                        String imageId = null;

                        if ("text".equals(messageType) && message.getText() != null) {
                            textBody = message.getText().getBody();
                            logger.info("Received text message from {}: {}", waId, textBody);
                        } else if ("image".equals(messageType) && message.getImage() != null) {
                            imageId = message.getImage().getId();
                            logger.info("Received image from {}: id={}", waId, imageId);
                        }

                        processMessageAsync(waId, messageType, textBody, imageId);
                    }

                    if (change.getValue().getStatuses() != null) {
                        for (WhatsAppWebhookRequest.Status status : change.getValue().getStatuses()) {
                            logger.info("Received status update: id={}, status={}, recipient={}",
                                    status.getId(), status.getStatus(), status.getRecipientId());
                        }
                    }
                }
            }

            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("EVENT_RECEIVED"); // Meta needs 200
        }
    }

    private void processMessageAsync(String waId, String messageType, String textBody, String imageId) {
        new Thread(() -> {
            try {
                whatsAppService.processMessage(waId, messageType, textBody, imageId);
            } catch (Exception e) {
                logger.error("Error processing message for {}: {}", waId, e.getMessage(), e);
            }
        }).start();
    }
}
