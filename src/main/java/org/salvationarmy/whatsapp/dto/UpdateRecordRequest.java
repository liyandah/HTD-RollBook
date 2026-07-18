package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.salvationarmy.whatsapp.entity.RecordStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecordRequest {
    private String corpsName;
    private String enrolledCorpsName;
    private String ward;
    private String brigade;
    private String firstName;
    private String familyName;
    private LocalDate dob;
    private Integer age;
    private String idNumber;
    @JsonAlias({"primaryContact", "primary_contact"})
    private String phoneNumber;
    @JsonAlias({"homeAddress", "home_address"})
    private String address;
    private String gender;
    private String maritalStatus;
    private Integer kidsCount;
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String favoriteSong;
    private String favoriteBibleVerse;
    @JsonAlias({"personImagePath"})
    private String personImageUrl;
    @JsonAlias({"certImagePath", "certificateImageUrl"})
    private String certImageUrl;
    private RecordStatus status;
}





