package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoRequestRequest {
    @NotBlank(message = "Reason is required")
    private String reason; // MISSING, UNCLEAR, WRONG_PERSON, OTHER
    
    private String notes; // Optional notes
}
