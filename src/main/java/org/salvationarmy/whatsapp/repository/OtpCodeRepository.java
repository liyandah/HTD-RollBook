package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findFirstByEmailAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime now);
    
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.email = :email AND o.createdAt > :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);

    /** Invalidate any previous unverified OTPs for this email so only the latest one works. */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.email = :email AND o.verified = false")
    void deleteUnverifiedByEmail(@Param("email") String email);
}
