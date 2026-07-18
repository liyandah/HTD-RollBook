package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private UUID id;
    private String email;
    private String fullName;
    private String role; // Added for frontend role checking
}
