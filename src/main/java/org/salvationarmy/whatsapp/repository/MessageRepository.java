package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.readAt IS NULL AND m.senderUserId != :userId")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt WHERE m.conversationId = :conversationId AND m.senderUserId != :userId AND m.readAt IS NULL")
    void markMessagesAsRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    Message findLastMessageByConversationId(@Param("conversationId") UUID conversationId);
}
