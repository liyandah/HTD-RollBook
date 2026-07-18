package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.ChatMessageResponse;
import org.salvationarmy.whatsapp.entity.ConversationNew;
import org.salvationarmy.whatsapp.entity.ConversationParticipant;
import org.salvationarmy.whatsapp.entity.Message;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.ConversationNewRepository;
import org.salvationarmy.whatsapp.repository.ConversationParticipantRepository;
import org.salvationarmy.whatsapp.repository.MessageRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationService {

    @Autowired
    private ConversationNewRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get or create bot conversation for user
     */
    @Transactional
    public ConversationNew getOrCreateBotConversation(UUID userId) {
        Optional<ConversationNew> existing = conversationRepository.findBotConversationByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Get bot user - try by fixed ID first (from migration), then by email
        UUID botUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        User botUser = userRepository.findById(botUserId).orElse(null);
        
        if (botUser == null) {
            // Try to find by email (migration might have created it with different ID)
            botUser = userRepository.findByEmail("bot@salvationarmy.org").orElse(null);
            if (botUser == null) {
                // Create new bot user if migration didn't run
                botUser = new User();
                botUser.setEmail("bot@salvationarmy.org");
                botUser.setFullName("HTF Data collection Bot");
                botUser.setRole("VIEWER");
                botUser.setStatus("ACTIVE");
                botUser = userRepository.save(botUser);
            }
            botUserId = botUser.getId();
        }

        // Create new bot conversation
        ConversationNew conversation = new ConversationNew();
        conversation.setType(ConversationNew.ConversationType.BOT);
        conversation = conversationRepository.save(conversation);

        // Add user as participant
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversationId(conversation.getId());
        participant.setUserId(userId);
        participantRepository.save(participant);

        // Add bot as participant
        ConversationParticipant botParticipant = new ConversationParticipant();
        botParticipant.setConversationId(conversation.getId());
        botParticipant.setUserId(botUserId);
        participantRepository.save(botParticipant);

        return conversation;
    }

    /**
     * Get or create direct conversation between two users
     */
    @Transactional
    public ConversationNew getOrCreateDirectConversation(UUID userId1, UUID userId2) {
        Optional<ConversationNew> existing = conversationRepository.findDirectConversationBetweenUsers(userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new direct conversation
        ConversationNew conversation = new ConversationNew();
        conversation.setType(ConversationNew.ConversationType.DIRECT);
        conversation = conversationRepository.save(conversation);

        // Add both users as participants
        ConversationParticipant p1 = new ConversationParticipant();
        p1.setConversationId(conversation.getId());
        p1.setUserId(userId1);
        participantRepository.save(p1);

        ConversationParticipant p2 = new ConversationParticipant();
        p2.setConversationId(conversation.getId());
        p2.setUserId(userId2);
        participantRepository.save(p2);

        return conversation;
    }

    /**
     * Get all conversations for a user
     */
    public List<ConversationNew> getUserConversations(UUID userId) {
        return conversationRepository.findByUserId(userId);
    }
    
    /**
     * Get conversation by ID
     */
    public Optional<ConversationNew> getConversationById(UUID conversationId) {
        return conversationRepository.findById(conversationId);
    }

    /**
     * Get messages for a conversation
     */
    public Page<Message> getConversationMessages(UUID conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    /**
     * Send a message
     */
    @Transactional
    public Message sendMessage(UUID conversationId, UUID senderUserId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderUserId(senderUserId);
        message.setMessageType(Message.MessageType.TEXT);
        message.setContent(content);
        message = messageRepository.save(message);

        // Update conversation last message time
        ConversationNew conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return message;
    }

    /**
     * Send bot message (no sender user)
     */
    @Transactional
    public Message sendBotMessage(UUID conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderUserId(null); // Bot messages have no sender
        message.setMessageType(Message.MessageType.TEXT);
        message.setContent(content);
        message = messageRepository.save(message);

        // Update conversation last message time
        ConversationNew conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return message;
    }

    /**
     * Mark messages as read
     */
    @Transactional
    public void markMessagesAsRead(UUID conversationId, UUID userId) {
        messageRepository.markMessagesAsRead(conversationId, userId, LocalDateTime.now());
    }

    /**
     * Get unread count for a conversation
     */
    public long getUnreadCount(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
    }

    /**
     * Get other participant in a direct conversation
     */
    public Optional<User> getOtherParticipant(UUID conversationId, UUID currentUserId) {
        List<UUID> otherIds = participantRepository.findOtherParticipantIds(conversationId, currentUserId);
        if (otherIds.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findById(otherIds.get(0));
    }
    
    @Autowired
    private BotRegistrationService botRegistrationService;
    
    /**
     * Process bot message - delegates to BotRegistrationService (legacy method for backward compatibility)
     */
    @Transactional
    public Message processBotMessage(UUID conversationId, UUID userId, String content) {
        return processBotMessage(conversationId, userId, content, null);
    }
    
    /**
     * Process bot message with client message ID for idempotency (legacy method)
     */
    @Transactional
    public Message processBotMessage(UUID conversationId, UUID userId, String content, UUID clientMessageId) {
        // Use new ChatMessageResponse and extract Message
        ChatMessageResponse chatResponse = botRegistrationService.processChatMessage(
                conversationId, userId, content, clientMessageId);
        
        // Find the message that was just created
        return messageRepository.findById(chatResponse.getMessageId())
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }
}
