package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
    
    Optional<ProcessedMessage> findByUserIdAndClientMessageId(UUID userId, UUID clientMessageId);
    
    @Modifying
    @Query("DELETE FROM ProcessedMessage p WHERE p.processedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
