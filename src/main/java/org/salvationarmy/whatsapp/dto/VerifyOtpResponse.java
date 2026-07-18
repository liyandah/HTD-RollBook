package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpResponse {
    private String token;
    private UUID userId;
    private String email;
    private String fullName;
    private Boolean requiresPassword; // true if user needs to create password
}
