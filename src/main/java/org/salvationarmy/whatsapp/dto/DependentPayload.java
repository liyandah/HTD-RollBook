package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DependentPayload {

    @NotBlank
    @JsonProperty("full_name")
    private String fullName;

    @NotNull
    private Integer age;

    private String relationship;

    private String gender;

    @JsonProperty("favorite_verse")
    private String favoriteVerse;

    private String address;

    private String phone;

    @JsonProperty("id_number")
    private String idNumber;
}
