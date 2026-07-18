package org.salvationarmy.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session", indexes = {
    @Index(name = "idx_chat_session_state", columnList = "state"),
    @Index(name = "idx_chat_session_status", columnList = "status"),
    @Index(name = "idx_chat_session_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "state", nullable = false, length = 50)
    private String state = "START";

    @Column(name = "corps_id")
    private Integer corpsId;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "corps_name", length = 255)
    private String corpsName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "age")
    private Integer age;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Column(name = "marital_status", length = 40)
    private String maritalStatus;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "next_of_kin_name", length = 100)
    private String nextOfKinName;

    @Column(name = "next_of_kin_phone", length = 20)
    private String nextOfKinPhone;

    @Column(name = "favorite_song", columnDefinition = "TEXT")
    private String favoriteSong;

    @Column(name = "bible_verse", columnDefinition = "TEXT")
    private String bibleVerse;

    @Column(name = "record_id")
    private java.util.UUID recordId;

    @Column(name = "ward", length = 255)
    private String ward;

    @Column(name = "brigade", length = 255)
    private String brigade;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "family_mode")
    private Boolean familyMode = false;

    @Column(name = "ask_marital_after_address")
    private Boolean askMaritalAfterAddress = false;

    @Column(name = "original_registrant_id")
    private java.util.UUID originalRegistrantId;

    @Column(name = "family_relation_type", length = 50)
    private String familyRelationType;

    @Column(name = "person_image_uploaded")
    private Boolean personImageUploaded = false;

    @Column(name = "cert_image_uploaded")
    private Boolean certImageUploaded = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, COMPLETE, CANCELLED

    @Column(name = "last_notified_step", length = 100)
    private String lastNotifiedStep;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
