package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    
    private String description;
    
    private BigDecimal targetAmount;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String status; // ACTIVE, COMPLETED, CANCELLED
}
