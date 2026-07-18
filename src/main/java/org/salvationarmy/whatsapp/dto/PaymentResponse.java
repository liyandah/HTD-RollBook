package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private String memberEmail;
    private UUID categoryId;
    private String categoryName;
    private String categoryType;
    private UUID projectId;
    private String projectName;
    private UUID eventId;
    private String eventName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
    private UUID recordedByUserId;
    private String recordedByName;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
}
