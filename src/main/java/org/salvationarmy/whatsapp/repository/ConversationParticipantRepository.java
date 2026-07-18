package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ConversationParticipant;
import org.salvationarmy.whatsapp.entity.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
    
    List<ConversationParticipant> findByUserId(UUID userId);
    
    List<ConversationParticipant> findByConversationId(UUID conversationId);
    
    @Query("SELECT cp.userId FROM ConversationParticipant cp WHERE cp.conversationId = :conversationId AND cp.userId != :excludeUserId")
    List<UUID> findOtherParticipantIds(@Param("conversationId") UUID conversationId, @Param("excludeUserId") UUID excludeUserId);
}
