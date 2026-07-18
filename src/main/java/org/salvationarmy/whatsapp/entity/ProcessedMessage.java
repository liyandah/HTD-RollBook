package org.salvationarmy.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_messages", indexes = {
    @Index(name = "idx_processed_msg_user_client", columnList = "user_id,client_message_id"),
    @Index(name = "idx_processed_msg_created", columnList = "processed_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "client_message_id", nullable = false)
    private UUID clientMessageId;
    
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;
    
    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;
    
    @CreatedDate
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
