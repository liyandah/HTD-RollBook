package org.salvationarmy.whatsapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.salvationarmy.whatsapp.dto.BotMessageRequest;
import org.salvationarmy.whatsapp.dto.BotMessageResponse;
import org.salvationarmy.whatsapp.service.ChatbotService;
import org.salvationarmy.whatsapp.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for in-house chatbot platform.
 * 
 * This endpoint handles all chatbot conversations and manages
 * the registration flow through a state machine.
 */
@RestController
@RequestMapping("/api/bot")
@Slf4j
public class BotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private FileService fileService;

    @PostMapping("/message")
    public ResponseEntity<BotMessageResponse> sendMessage(@RequestBody BotMessageRequest request) {
        try {
            // Validate request
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new BotMessageResponse("Error: Session ID is required.", "ERROR", "ERROR", null, null,
                                null, null, null, null, null, null));
            }

            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new BotMessageResponse("Error: Message cannot be empty.", "ERROR", "ERROR", null, null,
                                null, null, null, null, null, null));
            }

            // Process message
            ChatbotService.BotResponse response = chatbotService.processMessage(
                    request.getSessionId().trim(),
                    request.getMessage().trim()
            );

            return ResponseEntity.ok(new BotMessageResponse(
                    response.getReplyText(),
                    response.getState(),
                    response.getStatus(),
                    response.getDeclineReason(),
                    response.getChoices(),
                    response.getMemberStatus(),
                    response.getMemberFirstName(),
                    response.getMemberLastName(),
                    response.getMemberRecordCode(),
                    response.getMemberDepartment(),
                    response.getPersonImagePath()
            ));

        } catch (Exception e) {
            log.error("Error processing bot message: ", e);
            return ResponseEntity.ok(
                    new BotMessageResponse("Sorry, an error occurred. Please try again.", "ERROR", "ERROR", null, null,
                            null, null, null, null, null, null)
            );
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") String imageType) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session ID is required"));
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is required"));
            }

            if (!"person".equals(imageType) && !"cert".equals(imageType)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "imageType must be 'person' or 'cert'"));
            }

            // Save file and update record
            ChatbotService.BotResponse response = chatbotService.handleImageUpload(
                    sessionId.trim(),
                    file,
                    imageType
            );

            Map<String, Object> result = new HashMap<>();
            result.put("replyText", response.getReplyText());
            result.put("state", response.getState());
            result.put("status", response.getStatus());
            result.put("photoPath", response.getPhotoPath());
            result.put("success", true);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Error uploading image: ", e);
            return ResponseEntity.ok(
                    Map.of("error", "Failed to upload image. Please try again.",
                           "replyText", "Failed to upload image. Please try again.",
                           "success", false)
            );
        } catch (Exception e) {
            log.error("Error processing image upload: ", e);
            return ResponseEntity.ok(
                    Map.of("error", "An error occurred. Please try again.",
                           "replyText", "An error occurred. Please try again.",
                           "success", false)
            );
        }
    }
}
