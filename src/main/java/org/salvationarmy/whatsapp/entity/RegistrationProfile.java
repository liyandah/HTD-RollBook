package org.salvationarmy.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_profiles", indexes = {
    @Index(name = "idx_reg_profile_user", columnList = "user_id"),
    @Index(name = "idx_reg_profile_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "national_id", length = 100)
    private String nationalId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "next_of_kin_name", columnDefinition = "TEXT")
    private String nextOfKinName;

    @Column(name = "next_of_kin_phone", columnDefinition = "TEXT")
    private String nextOfKinPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.DRAFT; // DRAFT or COMPLETED

    @Column(name = "registration_step", length = 50)
    private String registrationStep = "ASK_PHONE"; // ASK_PHONE, ASK_NAME, ASK_ADDRESS, ASK_NOK_NAME, ASK_NOK_PHONE, CONFIRM, COMPLETED

    /** Primary member's soldier record code while registering a dependent (web chat proxy flow). */
    @Column(name = "proxy_parent_record_code", length = 50)
    private String proxyParentRecordCode;

    /** Relationship label for the dependent being registered (e.g. Child, Parent). */
    @Column(name = "proxy_relationship", length = 50)
    private String proxyRelationship;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RegistrationStatus {
        DRAFT, COMPLETED
    }
    
    public enum RegistrationStep {
        ASK_PHONE, ASK_NAME, ASK_ADDRESS, ASK_NOK_NAME, ASK_NOK_PHONE, CONFIRM, COMPLETED
    }
}
