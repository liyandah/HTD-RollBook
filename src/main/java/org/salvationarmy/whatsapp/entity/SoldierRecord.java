package org.salvationarmy.whatsapp.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "soldier_records", indexes = {
    @Index(name = "idx_soldier_wa_id", columnList = "wa_id"),
    @Index(name = "idx_soldier_status", columnList = "status"),
    @Index(name = "idx_soldier_created_at", columnList = "created_at"),
    @Index(name = "idx_soldier_record_code", columnList = "record_code"),
    @Index(name = "idx_soldier_template_type", columnList = "template_type"),
    @Index(name = "idx_soldier_ward", columnList = "ward"),
    @Index(name = "idx_soldier_brigade", columnList = "brigade")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoldierRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wa_id", nullable = false, length = 50)
    private String waId;

    @Column(name = "record_code", nullable = false, unique = true, length = 20)
    private String recordCode;

    @Column(name = "corps_name", length = 255)
    private String corpsName;

    @Column(name = "enrolled_corps_name", length = 255)
    private String enrolledCorpsName;

    @Column(name = "ward", length = 255)
    private String ward;

    @Column(name = "brigade", length = 255)
    private String brigade;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "family_name", length = 100)
    private String familyName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "age")
    private Integer age;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "marital_status", length = 40)
    private String maritalStatus;

    @Column(name = "kids_count", nullable = false)
    private Integer kidsCount = 0;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "brigade_eligibility", length = 64)
    private String brigadeEligibility;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "next_of_kin_name", length = 100)
    private String nextOfKinName;

    @Column(name = "next_of_kin_phone", length = 20)
    private String nextOfKinPhone;

    @Column(name = "favorite_song", columnDefinition = "TEXT")
    private String favoriteSong;

    @Column(name = "favorite_bible_verse", columnDefinition = "TEXT")
    private String favoriteBibleVerse;

    @Column(name = "person_image_path", length = 500)
    private String personImagePath;

    @Column(name = "cert_image_path", length = 500)
    private String certImagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_status", length = 50)
    private PhotoStatus photoStatus = PhotoStatus.MISSING;

    @Column(name = "photo_requested_at")
    private LocalDateTime photoRequestedAt;

    @Column(name = "photo_reviewed_at")
    private LocalDateTime photoReviewedAt;

    @Column(name = "photo_review_notes", columnDefinition = "TEXT")
    private String photoReviewNotes;

    @Column(name = "photo_requested_by")
    private UUID photoRequestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_requested_by", insertable = false, updatable = false)
    private User photoRequestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.IN_PROGRESS;

    @Column(name = "template_type", nullable = false, length = 50)
    private String templateType = "STANDARD";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "chat_session_id", length = 255)
    private String chatSessionId;

    @Column(name = "user_id")
    private UUID userId;

    /** National ID of the household head (links dependents to the proxy row). */
    @Column(name = "proxy_id", length = 50)
    private String proxyId;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "primary_registrant_id")
    private UUID primaryRegistrantId;

    @Column(name = "registration_relation", length = 50)
    private String registrationRelation;

    @Column(name = "needs_reupload", nullable = false)
    private Boolean needsReupload = false;

    @Column(name = "household_batch_id", length = 64)
    private String householdBatchId;

    @Column(name = "household_admin_notes", columnDefinition = "TEXT")
    private String householdAdminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PhotoStatus {
        MISSING,           // No photo uploaded
        UPLOADED,          // Photo uploaded, awaiting review
        APPROVED,          // Photo approved by admin
        REJECTED,          // Photo rejected by admin
        RESUBMIT_REQUESTED // Admin requested resubmission
    }
}






