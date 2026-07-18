package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SoldierRecordRepository extends JpaRepository<SoldierRecord, UUID>, 
                                                  JpaSpecificationExecutor<SoldierRecord> {
    
    Optional<SoldierRecord> findByWaIdAndStatus(String waId, RecordStatus status);
    
    List<SoldierRecord> findByWaId(String waId);
    
    long countByStatus(RecordStatus status);
    long countByStatusAndIsActiveTrue(RecordStatus status);
    long countByIsActiveTrue();
    
    @Query(value = "SELECT COUNT(*) FROM soldier_records WHERE COALESCE(is_active, TRUE) = TRUE AND dob IS NOT NULL AND EXTRACT(YEAR FROM AGE(dob)) < 16", nativeQuery = true)
    long countUnder16();
    
    @Query(value = "SELECT COUNT(*) FROM soldier_records WHERE COALESCE(is_active, TRUE) = TRUE AND dob IS NOT NULL AND EXTRACT(YEAR FROM AGE(dob)) >= 16", nativeQuery = true)
    long countAge16AndAbove();

    @Query(value = "SELECT COALESCE(SUM(COALESCE(kids_count, 0)), 0) FROM soldier_records WHERE COALESCE(is_active, TRUE) = TRUE", nativeQuery = true)
    long sumChildrenCount();
    
    @Query("SELECT s FROM SoldierRecord s WHERE s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<SoldierRecord> findRecentRecords(@Param("since") LocalDateTime since, Pageable pageable);
    
    // For monthly reports
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByStatusAndCreatedAtBetween(RecordStatus status, LocalDateTime start, LocalDateTime end);

    List<SoldierRecord> findByStatusOrderByCreatedAtDesc(RecordStatus status);
    List<SoldierRecord> findByStatusAndIsActiveTrueOrderByCreatedAtDesc(RecordStatus status);
    
    @Query(value = "SELECT nextval('soldier_record_seq')", nativeQuery = true)
    Long getNextRecordSequence();
    
    Optional<SoldierRecord> findByChatSessionId(String chatSessionId);
    
    Optional<SoldierRecord> findByRecordCode(String recordCode);
    
    List<SoldierRecord> findByUserIdAndStatus(UUID userId, RecordStatus status);
    
    List<SoldierRecord> findByUserId(UUID userId);

    @Query(value = "SELECT COUNT(*) FROM soldier_records WHERE TRIM(LOWER(id_number)) = TRIM(LOWER(CAST(:id AS VARCHAR))) "
            + "AND id_number IS NOT NULL AND TRIM(id_number) <> ''", nativeQuery = true)
    long countByNormalizedIdNumber(@Param("id") String idNumber);

    @Query(value = "SELECT COUNT(*) FROM soldier_records WHERE LOWER(TRIM(first_name)) = LOWER(TRIM(CAST(:fn AS VARCHAR))) "
            + "AND LOWER(TRIM(family_name)) = LOWER(TRIM(CAST(:ln AS VARCHAR)))", nativeQuery = true)
    long countByNormalizedFullName(@Param("fn") String firstName, @Param("ln") String familyName);

    @Query(value = "SELECT id_number FROM soldier_records WHERE LOWER(TRIM(first_name)) = LOWER(TRIM(CAST(:fn AS VARCHAR))) "
            + "AND LOWER(TRIM(family_name)) = LOWER(TRIM(CAST(:ln AS VARCHAR))) "
            + "AND id_number IS NOT NULL AND TRIM(id_number) <> '' LIMIT 1", nativeQuery = true)
    String findAnyIdNumberByNormalizedFullName(@Param("fn") String firstName, @Param("ln") String familyName);

    List<SoldierRecord> findByHouseholdBatchId(String householdBatchId);

    List<SoldierRecord> findByRecordCodeIn(Collection<String> recordCodes);

    @Query("SELECT s FROM SoldierRecord s WHERE "
            + "(LOWER(TRIM(s.idNumber)) = LOWER(TRIM(:pid)) AND s.proxyId IS NULL) "
            + "OR LOWER(TRIM(s.proxyId)) = LOWER(TRIM(:pid))")
    List<SoldierRecord> findHouseholdByProxyNationalId(@Param("pid") String proxyNationalId);

    Optional<SoldierRecord> findFirstByIdNumberIgnoreCase(String idNumber);

    List<SoldierRecord> findByPrimaryRegistrantId(UUID primaryRegistrantId);

    List<SoldierRecord> findByPrimaryRegistrantIdAndRegistrationRelationIgnoreCase(UUID primaryRegistrantId, String registrationRelation);
}


