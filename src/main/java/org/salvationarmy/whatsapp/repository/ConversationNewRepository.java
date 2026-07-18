package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ConversationNew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationNewRepository extends JpaRepository<ConversationNew, UUID> {
    
    @Query("SELECT DISTINCT c FROM ConversationNew c " +
           "JOIN ConversationParticipant cp ON c.id = cp.conversationId " +
           "WHERE cp.userId = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<ConversationNew> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT DISTINCT c FROM ConversationNew c " +
           "JOIN ConversationParticipant cp1 ON c.id = cp1.conversationId " +
           "JOIN ConversationParticipant cp2 ON c.id = cp2.conversationId " +
           "WHERE cp1.userId = :userId1 AND cp2.userId = :userId2 AND c.type = 'DIRECT'")
    Optional<ConversationNew> findDirectConversationBetweenUsers(
            @Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
    
    @Query("SELECT DISTINCT c FROM ConversationNew c " +
           "JOIN ConversationParticipant cp ON c.id = cp.conversationId " +
           "WHERE cp.userId = :userId AND c.type = 'BOT'")
    Optional<ConversationNew> findBotConversationByUserId(@Param("userId") UUID userId);
}
