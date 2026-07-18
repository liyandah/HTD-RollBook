package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);
}
