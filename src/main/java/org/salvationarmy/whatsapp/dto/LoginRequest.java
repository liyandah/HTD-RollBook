package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @JsonAlias("username")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}






