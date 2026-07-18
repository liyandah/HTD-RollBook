package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Email(message = "Email must be valid")
    private String email;
    
    private String fullName;
    private String role;
    private String status;
}





