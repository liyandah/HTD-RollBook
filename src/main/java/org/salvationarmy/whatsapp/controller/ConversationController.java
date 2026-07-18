package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.*;
import org.salvationarmy.whatsapp.entity.ConversationNew;
import org.salvationarmy.whatsapp.entity.Message;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.RegistrationProfileRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationProfileRepository registrationProfileRepository;

    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConversationController.class);
        logger.debug("Getting user ID for identifier: {}", identifier);
        
        // Try email first
        java.util.Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isPresent()) {
            logger.debug("Found user by email: {} with ID: {}", identifier, userOpt.get().getId());
            return userOpt.get().getId();
        }
        // Try username if email lookup failed
        userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isPresent()) {
            logger.debug("Found user by username: {} with ID: {}", identifier, userOpt.get().getId());
            return userOpt.get().getId();
        }
        // User doesn't exist - create it (this can happen if user was deleted or DB was reset)
        logger.warn("User not found for identifier: {}. Creating new user...", identifier);
        User newUser = new User();
        newUser.setEmail(identifier.contains("@") ? identifier : identifier + "@salvationarmy.org");
        newUser.setUsername(identifier.contains("@") ? identifier.split("@")[0] : identifier);
        newUser.setFullName(identifier.contains("@") ? identifier.split("@")[0] : identifier); // Use email prefix as name
        newUser.setRole("VIEWER");
        newUser.setStatus("ACTIVE");
        newUser = userRepository.save(newUser);
        logger.info("Created new user: {} with ID: {}", identifier, newUser.getId());
        return newUser.getId();
    }

    @GetMapping
    public ResponseEntity<?> getConversations(Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Ensure bot conversation exists for this user (creates if doesn't exist)
            ConversationNew botConv;
            try {
                botConv = conversationService.getOrCreateBotConversation(userId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                    .error("Failed to get/create bot conversation for user {}: {}", userId, e.getMessage(), e);
                e.printStackTrace();
                // Set to null and continue - we'll try to get conversations anyway
                botConv = null;
            }
            
            List<ConversationNew> conversations;
            try {
                conversations = conversationService.getUserConversations(userId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                    .error("Failed to get user conversations: {}", e.getMessage(), e);
                // Return empty list if query fails
                conversations = new java.util.ArrayList<>();
            }
            
            // Ensure bot conversation is in the list (it should be, but double-check)
            final ConversationNew finalBotConv = botConv; // Make effectively final for lambda
            if (finalBotConv != null) {
                boolean hasBot = conversations.stream()
                        .anyMatch(c -> c.getType() == ConversationNew.ConversationType.BOT && c.getId().equals(finalBotConv.getId()));
                if (!hasBot) {
                    conversations.add(0, finalBotConv); // Add at beginning if missing
                }
            }

            List<ConversationResponse> responses = conversations.stream().map(conv -> {
                ConversationResponse resp = new ConversationResponse();
                resp.setId(conv.getId());
                resp.setType(conv.getType().name());
                resp.setLastMessageAt(conv.getLastMessageAt());
                resp.setCreatedAt(conv.getCreatedAt());

                // Get last message preview (with error handling)
                try {
                    Page<Message> lastMessages = conversationService.getConversationMessages(
                            conv.getId(), PageRequest.of(0, 1));
                    if (lastMessages != null && lastMessages.hasContent()) {
                        Message lastMsg = lastMessages.getContent().get(0);
                        if (lastMsg != null && lastMsg.getContent() != null) {
                            String preview = lastMsg.getContent();
                            resp.setLastMessagePreview(preview.length() > 50 
                                    ? preview.substring(0, 50) + "..." 
                                    : preview);
                        }
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                        .warn("Failed to get last message for conversation {}: {}", conv.getId(), e.getMessage());
                }

                // Get unread count (with error handling)
                try {
                    resp.setUnreadCount(conversationService.getUnreadCount(conv.getId(), userId));
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                        .warn("Failed to get unread count for conversation {}: {}", conv.getId(), e.getMessage());
                    resp.setUnreadCount(0);
                }

                // Get other participant for DIRECT conversations
                if (conv.getType() == ConversationNew.ConversationType.DIRECT) {
                    try {
                        conversationService.getOtherParticipant(conv.getId(), userId)
                                .ifPresent(other -> {
                                    UserInfo userInfo = new UserInfo(
                                            other.getId(),
                                            other.getEmail(),
                                            other.getFullName(),
                                            other.getRole()
                                    );
                                    resp.setOtherParticipant(userInfo);
                                });
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                            .warn("Failed to get other participant for conversation {}: {}", conv.getId(), e.getMessage());
                    }
                }

                return resp;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ConversationController.class)
                .error("Error getting conversations: {}", e.getMessage(), e);
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to load conversations");
            error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
            error.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messages = conversationService.getConversationMessages(id, pageable);

            // Mark as read
            conversationService.markMessagesAsRead(id, userId);

            List<MessageResponse> responses = messages.getContent().stream().map(msg -> {
                MessageResponse resp = new MessageResponse();
                resp.setId(msg.getId());
                resp.setConversationId(msg.getConversationId());
                resp.setSenderUserId(msg.getSenderUserId());
                resp.setMessageType(msg.getMessageType().name());
                resp.setContent(msg.getContent());
                resp.setCreatedAt(msg.getCreatedAt());
                resp.setReadAt(msg.getReadAt());
                resp.setBot(msg.getSenderUserId() == null);

                // Get sender name
                if (msg.getSenderUserId() != null) {
                    userRepository.findById(msg.getSenderUserId())
                            .ifPresent(user -> resp.setSenderName(user.getFullName() != null 
                                    ? user.getFullName() 
                                    : user.getEmail()));
                } else {
                    resp.setSenderName("Bot");
                }

                return resp;
            }).collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("content", responses);
            result.put("totalPages", messages.getTotalPages());
            result.put("totalElements", messages.getTotalElements());
            result.put("currentPage", page);

            // For bot conversations, include current registration step so frontend stays in sync
            conversationService.getConversationById(id)
                    .filter(c -> c.getType() == ConversationNew.ConversationType.BOT)
                    .ifPresent(c -> registrationProfileRepository.findByUserId(userId)
                            .ifPresent(profile -> result.put("registrationStep", profile.getRegistrationStep())));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Check if this is a bot conversation
            ConversationNew conversation = conversationService.getConversationById(id)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            if (conversation.getType() == ConversationNew.ConversationType.BOT) {
                // For bot conversations: save user message first, then process bot response
                Message userMessage = conversationService.sendMessage(id, userId, request.getContent());
                
                // Process bot message and get bot response (with clientMessageId for idempotency)
                Message botResponse = conversationService.processBotMessage(
                        id, userId, request.getContent(), request.getClientMessageId());
                
                // Return the bot response (user will see their message from the reload)
                MessageResponse response = new MessageResponse();
                response.setId(botResponse.getId());
                response.setConversationId(botResponse.getConversationId());
                response.setSenderUserId(botResponse.getSenderUserId());
                response.setMessageType(botResponse.getMessageType().name());
                response.setContent(botResponse.getContent());
                response.setCreatedAt(botResponse.getCreatedAt());
                response.setBot(true);
                response.setSenderName("HTF Data collection Bot");
                
                return ResponseEntity.ok(response);
            } else {
                // Regular user message
                Message message = conversationService.sendMessage(id, userId, request.getContent());

                MessageResponse response = new MessageResponse();
                response.setId(message.getId());
                response.setConversationId(message.getConversationId());
                response.setSenderUserId(message.getSenderUserId());
                response.setMessageType(message.getMessageType().name());
                response.setContent(message.getContent());
                response.setCreatedAt(message.getCreatedAt());
                response.setBot(false);

                userRepository.findById(userId)
                        .ifPresent(user -> response.setSenderName(
                                user.getFullName() != null ? user.getFullName() : user.getEmail()));

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/direct")
    public ResponseEntity<ConversationResponse> createDirectConversation(
            @RequestParam UUID targetUserId,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            ConversationNew conversation = conversationService.getOrCreateDirectConversation(userId, targetUserId);

            ConversationResponse response = new ConversationResponse();
            response.setId(conversation.getId());
            response.setType(conversation.getType().name());
            response.setCreatedAt(conversation.getCreatedAt());

            conversationService.getOtherParticipant(conversation.getId(), userId)
                    .ifPresent(other -> {
                        UserInfo userInfo = new UserInfo(
                                other.getId(),
                                other.getEmail(),
                                other.getFullName(),
                                other.getRole()
                        );
                        response.setOtherParticipant(userInfo);
                    });

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bot")
    public ResponseEntity<ConversationResponse> getBotConversation(Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            ConversationNew conversation = conversationService.getOrCreateBotConversation(userId);

            ConversationResponse response = new ConversationResponse();
            response.setId(conversation.getId());
            response.setType(conversation.getType().name());
            response.setLastMessageAt(conversation.getLastMessageAt());
            response.setCreatedAt(conversation.getCreatedAt());
            response.setUnreadCount(conversationService.getUnreadCount(conversation.getId(), userId));

            // Get last message preview
            Page<Message> lastMessages = conversationService.getConversationMessages(
                    conversation.getId(), PageRequest.of(0, 1));
            if (lastMessages.hasContent()) {
                Message lastMsg = lastMessages.getContent().get(0);
                String preview = lastMsg.getContent();
                response.setLastMessagePreview(preview.length() > 50 
                        ? preview.substring(0, 50) + "..." 
                        : preview);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
