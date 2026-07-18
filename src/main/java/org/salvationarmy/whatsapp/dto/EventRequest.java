package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    @NotBlank(message = "Event name is required")
    private String name;
    
    private String description;
    
    private String eventType; // EASTER_CAMP, YOUTH_CAMP, CONGRESS, etc.
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String location;
    
    private String status; // ACTIVE, COMPLETED, CANCELLED
}
