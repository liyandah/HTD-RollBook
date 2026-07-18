package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProxyUserPayload {

    @NotBlank
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank
    private String phone;

    private String address;

    @NotBlank
    @JsonProperty("id_number")
    private String idNumber;

    /** Optional; if absent, a default adult DOB is used for membership classification only. */
    private LocalDate dob;
}
