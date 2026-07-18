package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoRejectRequest {
    @NotBlank(message = "Notes are required when rejecting a photo")
    private String notes; // Required notes explaining rejection
}
