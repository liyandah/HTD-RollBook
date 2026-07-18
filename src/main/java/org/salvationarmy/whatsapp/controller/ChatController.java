package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.ChatMessageRequest;
import org.salvationarmy.whatsapp.dto.ChatMessageResponse;
import org.salvationarmy.whatsapp.entity.ConversationNew;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.BotRegistrationService;
import org.salvationarmy.whatsapp.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Controller for chat messages - handles bot registration flow
 * Backend controls 100% of the flow
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private BotRegistrationService botRegistrationService;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Process chat message - single endpoint that handles everything
     * POST /api/chat/message
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> processMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Verify conversation exists and is a bot conversation
            ConversationNew conversation = conversationService.getConversationById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            if (conversation.getType() != ConversationNew.ConversationType.BOT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ChatMessageResponse("This endpoint is only for bot conversations", null, null, null, null, null));
            }
            
            // Save user message first
            conversationService.sendMessage(request.getConversationId(), userId, request.getText());
            
            // Process bot message and get response
            ChatMessageResponse response = botRegistrationService.processChatMessage(
                    request.getConversationId(),
                    userId,
                    request.getText(),
                    request.getClientMessageId()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatMessageResponse("An error occurred: " + e.getMessage(), null, null, null, null, null));
        }
    }
    
    /**
     * Upload image for post-verification flow
     * POST /api/chat/upload-image
     */
    @PostMapping("/upload-image")
    public ResponseEntity<ChatMessageResponse> uploadImage(
            @RequestParam("conversationId") UUID conversationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") String imageType,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Verify conversation exists and is a bot conversation
            ConversationNew conversation = conversationService.getConversationById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            if (conversation.getType() != ConversationNew.ConversationType.BOT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ChatMessageResponse("This endpoint is only for bot conversations", null, null, null, null, null));
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ChatMessageResponse("File is required", null, null, null, null, null));
            }
            
            if (!"person".equals(imageType) && !"cert".equals(imageType)) {
                return ResponseEntity.badRequest()
                        .body(new ChatMessageResponse("imageType must be 'person' or 'cert'", null, null, null, null, null));
            }
            
            // Process image upload
            ChatMessageResponse response = botRegistrationService.handleImageUpload(
                    conversationId,
                    userId,
                    file,
                    imageType
            );
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatMessageResponse("Failed to upload image: " + e.getMessage(), null, null, null, null, null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatMessageResponse("An error occurred: " + e.getMessage(), null, null, null, null, null));
        }
    }
    
    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        // Try email first
        java.util.Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        // Try username if email lookup failed
        userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        // Create user if doesn't exist (treat identifier as email)
        User newUser = new User();
        newUser.setEmail(identifier.contains("@") ? identifier : identifier + "@salvationarmy.org");
        newUser.setUsername(identifier.contains("@") ? identifier.split("@")[0] : identifier);
        newUser.setFullName(identifier.contains("@") ? identifier.split("@")[0] : identifier);
        newUser.setRole("VIEWER");
        newUser.setStatus("ACTIVE");
        User savedUser = userRepository.save(newUser);
        return savedUser.getId();
    }
}
