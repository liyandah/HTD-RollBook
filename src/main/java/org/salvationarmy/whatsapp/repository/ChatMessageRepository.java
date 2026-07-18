package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);
}
