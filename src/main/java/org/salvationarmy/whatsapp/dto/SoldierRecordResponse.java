package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldierRecordResponse {
    private UUID id;
    private String waId;
    private String recordCode;
    private String corpsName;
    private String enrolledCorpsName;
    private String ward;
    private String brigade;
    private String firstName;
    private String familyName;
    private LocalDate dob;
    private Integer age;
    private String idNumber;
    private String gender;
    private String maritalStatus;
    private Integer kidsCount;
    private String department;
    private String brigadeEligibility;
    private String phoneNumber;
    private String address;
    private String homeAddress;
    private String nextOfKinName;
    private String nextOfKinPhone;
    /** National ID of household head when this row is a dependent. */
    private String proxyId;
    private String relationship;
    private UUID primaryRegistrantId;
    private String registrationRelation;
    private Boolean needsReupload;
    private String householdBatchId;
    private String registeredByName;
    private String proxyContact;
    private String householdAdminNotes;
    private String favoriteSong;
    private String favoriteBibleVerse;
    private String personImageUrl;
    private String certImageUrl;
    private String status;
    private String templateType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}






