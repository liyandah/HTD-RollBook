package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapturePaymentRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    
    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    
    private UUID projectId; // Optional, only for PROJECT type
    
    private UUID eventId; // Optional, only for EVENT type
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency = "USD";
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod; // CASH, ECOCASH, BANK_TRANSFER, etc.
    
    private String referenceNumber;
    
    private String notes;
}
