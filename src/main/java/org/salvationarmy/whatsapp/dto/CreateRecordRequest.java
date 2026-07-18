package org.salvationarmy.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecordRequest {
    @NotBlank(message = "WhatsApp ID is required")
    private String waId;
    
    private String corpsName;
    private String enrolledCorpsName;
    private String ward;
    private String brigade;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Family name is required")
    private String familyName;
    
    @NotNull(message = "Date of birth is required")
    private LocalDate dob;
    
    private String idNumber;

    /** Optional: Male / Female / M / F — used for fellowship classification */
    private String gender;
    /** Optional: Single, Married, Divorced, Widow — used for classification */
    private String maritalStatus;
    /** Optional: number of children (default 0) */
    private Integer kidsCount;

    private String favoriteSong;
    private String favoriteBibleVerse;
    private RecordStatus status; // Defaults to IN_PROGRESS if not provided
}





